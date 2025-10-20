package clip.derivation

import clip.derivation.Api
import clip.dispatch.Command
import clip.dispatch.InvocationResult
import scala.collection.mutable.ListBuffer
import scala.quoted.Expr
import scala.quoted.Quotes
import scala.quoted.Type
import clip.dispatch.EagerResult
import clip.dispatch.Invoke
import clip.dispatch.InvokeEager

private class macros(using val qctx: Quotes):
  import qctx.reflect.*

  def getDefaultParams(method: Symbol): Map[Symbol, Expr[() => ?]] =
    val pairs = for
      (param, idx) <- method.paramSymss.flatten.zipWithIndex
      if (param.flags.is(Flags.HasDefault))
    yield
      val term = if method.isClassConstructor then
        val defaultName = s"$$lessinit$$greater$$default$$${idx + 1}"
        Ref(method.owner.companionModule.methodMember(defaultName).head)
      else
        val defaultName = s"${method.name}$$default$$${idx + 1}"
        Ref(method.owner.methodMember(defaultName).head)
      param.termRef.widenTermRefByName.asType match
        case '[t] =>
          param -> '{ () =>
            ${ term.asExprOf[t] }
          }
    pairs.toMap

  def callOrInstantiate(
      instance: Term,
      methodOrClass: Symbol,
      args: List[Term]
  ): Term =
    val base: Term =
      if methodOrClass.isClassDef then
        // Select.unique(New(instance.select(methodOrClass).tpe)), "<init>")
        Select.unique(
          New(Inferred(instance.tpe.select(methodOrClass))),
          "<init>"
        )
      else instance.select(methodOrClass)

    val argsIt = args.iterator
    val paramss =
      if methodOrClass.isClassDef then
        methodOrClass.primaryConstructor.paramSymss
      else methodOrClass.paramSymss

    val argss =
      for params <- paramss yield for param <- params yield argsIt.next()

    assert(
      !argsIt.hasNext,
      "Not all arguments were consumed in callOrInstantiate"
    )

    val application = argss.foldLeft(base)((lhs, args) => Apply(lhs, args))
    application

  def initialValue(tpe: TypeRepr): Term =
    if tpe =:= TypeRepr.of[Byte] then Literal(ByteConstant(0))
    else if tpe =:= TypeRepr.of[Short] then Literal(ShortConstant(0))
    else if tpe =:= TypeRepr.of[Int] then Literal(IntConstant(0))
    else if tpe =:= TypeRepr.of[Long] then Literal(LongConstant(0L))
    else if tpe =:= TypeRepr.of[Float] then Literal(FloatConstant(0.0f))
    else if tpe =:= TypeRepr.of[Double] then Literal(DoubleConstant(0.0))
    else if tpe =:= TypeRepr.of[Char] then Literal(CharConstant('\u0000'))
    else if tpe =:= TypeRepr.of[Boolean] then Literal(BooleanConstant(false))
    else Literal(NullConstant())

  /** What is known statically about a command/group method
    *
    * This case class gets most of its fields from an `@command/@group` and thus
    * should contain fields named the same as those in the annotation for
    * clarity (where applicable, and of course where values are represented as
    * `Expr`s).
    *
    * Backwards compatibility is not super important here since CommandInfo is
    * only used at compile-time, so we can freely add/remove fields as needed.
    * Hence, the suggested convention is to declare parameters in the order of
    * the command/group annotations, followed by other fields.
    */
  case class CommandInfo(
      // annotation fields
      name: Expr[String],
      help: Expr[String],
      version: Option[Expr[String]],
      subcommands: Expr[Seq[Command[?, ?]]],
      // non-annotation fields
      isGroup: Boolean,
      methodSymbol: Symbol,
      ctxType: TypeRepr,
      returnType: TypeRepr,
      paramInfos: List[ParamInfo],
      invoke: Expr[Invoke[?, Any]]
  )

  def commandInfosForAnnot(method: Symbol, annot: Term): CommandInfo =
    val (name0, help, versionExpr, subs, isGroup) = annot.asExpr match
      case '{ command($name, $help, $version) } =>
        (name, help, version, '{ Seq() }, false)
      case '{ group($name, $help, $version, $subs) } =>
        (name, help, version, subs, true)

    val name = name0.asTerm match
      case term if term.symbol.name.contains("$default$") =>
        Expr(clip.util.text.kebabify(method.name))
      case _ =>
        name0

    // Note 1: we can't call `valueOrAbort` directly on the version
    // expression and compare with null, because a default parameter will
    // not be inlined, but rather passed as a reference to a synthetic
    // method containing the name "$default".
    //
    // Note 2: we want to check if the version is "unset" (i.e., left to
    // default). In light of Note 1, The way we do this is slightly hacky:
    // we simply check if the expression's term symbol name contains
    // "$default". This works in practice, but it is technically possible
    // that through some clever mechanism a *different* expression with a
    // name containing "$default" could be passed in. If that is ever the
    // case, we'd need to compare if the versionExpr's symbol actually
    // matches the one declared in the annotation's default parameters.
    val version: Option[Expr[String]] =
      if versionExpr.asTerm.symbol.name.contains("$default$") then None
      else Some(versionExpr)

    val pinfos = paramInfosForMethod(method)
    val invokeFn = invoke(method, pinfos)
    invokeFn.asTerm.tpe.asType match
      case '[Invoke[ctxType, resType]] =>
        CommandInfo(
          name = name,
          help = help,
          version = version,
          subcommands = subs,
          isGroup = isGroup,
          methodSymbol = method,
          ctxType = TypeRepr.of[ctxType],
          returnType = TypeRepr.of[resType],
          paramInfos = pinfos,
          invoke = invokeFn
        )

  def commandInfosForMethod(method: Symbol): List[CommandInfo] =
    val annots = method.annotations.filter(annot =>
      annot.tpe <:< TypeRepr.of[clip.derivation.command] ||
        annot.tpe <:< TypeRepr.of[clip.derivation.group]
    )
    for annot <- annots yield commandInfosForAnnot(method, annot)

  def commandInfosFromContainer(container: Term): List[CommandInfo] =
    for
      method <- container.tpe.typeSymbol.methodMembers
      cmd <- commandInfosForMethod(method)
    yield cmd

  def commandInfoAsCommand(
      info: CommandInfo,
      generatedChildren: List[Expr[Command[?, ?]]],
      root: Boolean = false
  ): Expr[Command[?, ?]] =
    (info.ctxType.asType, info.returnType.asType) match
      case ('[ctxType], '[resType]) =>
        val eagers = collection.mutable.ListBuffer.empty[
          Expr[(clip.dispatch.EagerParam)]
        ]

        eagers += '{ clip.dispatch.StandardEagerParams.help() }
        if info.version.isDefined then
          eagers += '{
            clip.dispatch.StandardEagerParams.version(${ info.version.get })
          }
        if root then
          eagers += '{ clip.dispatch.StandardEagerParams.completion() }
          eagers += '{ clip.dispatch.StandardEagerParams.color() }

        '{
          val children: Map[String, Command[resType, ?]] =
            (
              ${ info.subcommands }.asInstanceOf[Seq[Command[resType, ?]]] ++
                ${
                  Expr.ofSeq(
                    generatedChildren.map(_.asExprOf[Command[resType, ?]])
                  )
                }
            ).map(cmd => cmd.name -> cmd).toMap

          val eag = ${ Expr.ofList(eagers.toList) }

          Command[ctxType, resType](
            name = ${ info.name },
            help = ${ info.help },
            listChildren = () => children.values.toSeq,
            getChild = (name: String) => children.get(name),
            params = ${
              Expr.ofSeq(
                info.paramInfos.collect:
                  case arg: ParamInfo.Arg =>
                    '{
                      clip.dispatch.Param(
                        names = Seq(${ arg.name }) ++ ${ arg.aliases },
                        argName = ${
                          if arg.flag then '{ None }
                          else '{ Some(${ arg.reader }.typeName) }
                        },
                        endOfNamed = false,
                        repeats = ${
                          if arg.collectionFactory.isDefined then Expr(true)
                          else Expr(false)
                        },
                        help = ${ arg.help },
                        completer = ${ arg.completer }
                      )
                    }
              )
            } ++ eag.map(_._1),
            terminal = ${ Expr(!info.isGroup) },
            invoke = ${ info.invoke.asExprOf[Invoke[ctxType, resType]] },
            eagerInvokes = eag.map(_._2)
          )
        }
      case _ => sys.error("unreachable")

  enum ParamInfo:
    case Arg(
        name: Expr[String],
        aliases: Expr[Seq[String]],
        help: Expr[String],
        reader: Expr[Api#Reader[?]],
        collectionFactory: Option[Expr[scala.collection.Factory[?, ?]]],
        flag: Boolean,
        symbol: Symbol,
        api: Expr[Api],
        default: Option[Expr[() => ?]],
        completer: Expr[clip.completion.Completer]
    )
    case Context(symbol: Symbol)

  def paramInfoForParam(
      symbol: Symbol,
      defaults: Map[Symbol, Expr[() => ?]]
  ): ParamInfo =

    def elemReader(
        elemTpe: TypeRepr,
        apiExpr: Expr[Api],
        repeats: Boolean
    ): Expr[Api#Reader[?]] =
      val readerType =
        TypeSelect(apiExpr.asTerm, "Reader").tpe.appliedTo(elemTpe)
      Implicits.search(readerType) match
        case iss: ImplicitSearchSuccess =>
          iss.tree.asExprOf[Api#Reader[?]]
        case _ =>
          var message =
            s"Could not find implicit Reader for parameter '${symbol.name}' of type ${elemTpe.show}"
          if !repeats && symbol.termRef.widenTermRefByName <:< TypeRepr
              .of[Iterable[?]]
          then
            message += s" Note: parameter '${symbol.name}' is of an Iterable type but not marked as repeated in the @arg annotation."
          report.error(message, symbol.pos.get)
          '{ ??? }

    def paramCompleter(
        elemTpe: TypeRepr,
        apiExpr: Expr[Api]
    ) =
      val completerType =
        TypeSelect(apiExpr.asTerm, "CompleterFor").tpe.appliedTo(elemTpe)
      Implicits.search(completerType) match
        case iss: ImplicitSearchSuccess =>
          '{
            ${
              iss.tree.asExprOf[clip.completion.CompletionApi#CompleterFor[?]]
            }.completer
          }
        case _ =>
          '{ clip.completion.Completer.Empty }

    val annots = symbol.annotations.collect:
      case annot if annot.tpe <:< TypeRepr.of[Api#arg]                 => annot
      case annot if annot.tpe <:< TypeRepr.of[clip.derivation.context] =>
        annot

    annots match
      case Nil =>
        ParamInfo.Arg(
          name =
            if defaults.contains(symbol) then
              '{ "--" + clip.util.text.kebabify(${ Expr(symbol.name) }) }
            else '{ clip.util.text.kebabify(${ Expr(symbol.name) }) },
          aliases = '{ Seq() },
          help = '{ "" },
          reader = elemReader(
            symbol.termRef.widenTermRefByName,
            '{ clip.derivation.default },
            false
          ),
          collectionFactory = None,
          flag = symbol.termRef.widenTermRefByName <:< TypeRepr.of[Boolean],
          symbol = symbol,
          api = '{ clip.derivation.default },
          default = defaults.get(symbol),
          completer = paramCompleter(
            symbol.termRef.widenTermRefByName,
            '{ clip.derivation.default }
          )
        )
      case head :: Nil if head.tpe <:< TypeRepr.of[Api#arg] =>
        // TODO: ideally we'd use `Expr` pattern matching to extract the
        // annotation arguments and api term:
        //
        // head match case '{$api.arg($name, $aliases, $help, $repeats, $reader,
        // ...)} =>
        //
        // but unfortunately I haven't been able to figure out a way to match on
        // path-dependent types, so for now we manually deconstruct the tree
        val TypeRef(apiTerm: TermRef, _) = head.tpe: @unchecked
        val apiExpr = Ref.term(apiTerm).asExprOf[Api]
        val annotArgs = head match
          case Apply(base, args) =>
            args
          case Inlined(_, _, Apply(_, args)) =>
            args
          case _ =>
            report.errorAndAbort(
              s"Unexpected structure for annotation: ${head}",
              symbol.pos.get
            )

        // indices correspond to the order of parameters in the `arg` annotation
        val annotName = annotArgs(0).asExprOf[String]
        val annotAliases = annotArgs(1).asExprOf[Seq[String]]
        val annotHelp = annotArgs(2).asExprOf[String]
        val annotRepeats = annotArgs(3).asExprOf[Boolean]
        val annotReader = annotArgs(4).asExprOf[Api#Reader[?]]
        val annotCompleter = annotArgs(5).asExprOf[clip.completion.Completer]

        val repeats =
          if annotRepeats.asTerm.symbol.name.contains("$default$") then false
          else annotRepeats.valueOrAbort

        val (elemTpe, colFactoryOpt) = if repeats then
          val tpe = symbol.termRef.widenTermRefByName
          tpe.baseType(TypeRepr.of[Iterable[?]].typeSymbol) match
            case AppliedType(_, List(elemType0)) =>
              val elemType = elemType0.widenTermRefByName
              val colType = tpe
              val factoryType = TypeRepr
                .of[scala.collection.Factory]
                .appliedTo(List(elemType, colType))
              Implicits.search(factoryType) match
                case success: ImplicitSearchSuccess =>
                  val factory =
                    success.tree.asExprOf[scala.collection.Factory[?, ?]]
                  (elemType, Some(factory))
                case failure: ImplicitSearchFailure =>
                  report.error(
                    s"Could not find factory for collection type: ${colType.show}",
                    symbol.pos.get
                  )
                  (elemType, None)
            case _ =>
              report.error(
                "Parameter is marked as repeated but is not of Iterable type",
                symbol.pos.get
              )
              (symbol.termRef.widenTermRefByName, None)
        else (symbol.termRef.widenTermRefByName, None)

        val reader: Expr[Api#Reader[?]] =
          if annotReader.asTerm.symbol.name.contains("$default$") then
            elemReader(elemTpe, apiExpr, repeats)
          else annotReader

        val completer: Expr[clip.completion.Completer] =
          if annotCompleter.asTerm.symbol.name.contains("$default$") then
            paramCompleter(elemTpe, apiExpr)
          else annotCompleter

        ParamInfo.Arg(
          name = annotName,
          aliases = annotAliases,
          help = annotHelp,
          reader = reader,
          collectionFactory = colFactoryOpt,
          flag = symbol.termRef.widen <:< TypeRepr.of[Boolean],
          symbol = symbol,
          api = apiExpr,
          default = defaults.get(symbol),
          completer = completer
        )
      case head :: Nil if head.tpe <:< TypeRepr.of[clip.derivation.context] =>
        ParamInfo.Context(symbol)
      case more =>
        report.errorAndAbort(
          s"Multiple @arg/@context annotations found on parameter ${symbol.name}",
          symbol.pos.get
        )

  def paramInfosForMethod(method: Symbol): List[ParamInfo] =
    val defaults = getDefaultParams(method)
    for paramSymbol <- method.paramSymss.flatten
    yield paramInfoForParam(paramSymbol, defaults)

  def invoke(method: Symbol, params: Seq[ParamInfo]): Expr[Invoke[?, Any]] =
    var hasCtx = false
    var ctxType: TypeRepr = TypeRepr.of[Any]
    for param <- params do
      param match
        case ParamInfo.Context(symbol) =>
          if !hasCtx then
            hasCtx = true
            ctxType = symbol.termRef.widenTermRefByName
          else
            report.errorAndAbort(
              s"Multiple @context annotations found in method ${method.name}. Only one is allowed per method.",
              method.pos.get
            )
        case _ =>

    val retType = method.termRef.widen match
      case MethodType(_, _, retType) => retType
      case _                         =>
        report.errorAndAbort(
          s"Expected method type for ${method.name}",
          method.pos.get
        )

    (ctxType.asType, retType.asType) match
      case ('[ctxType], '[retType]) =>
        val invokeFn = '{
          (
              chain: Seq[Command[?, ?]],
              ctx: ctxType,
              args: collection.Map[String, Iterable[clip.getopt.Arg]]
          ) =>
            val paramErrors = ListBuffer.empty[InvocationResult.ParamError]

            ${
              val valSyms =
                for param <- method.paramSymss.flatten
                yield Symbol.newVal(
                  Symbol.spliceOwner,
                  param.name,
                  param.termRef.widenTermRefByName,
                  Flags.Mutable,
                  Symbol.noSymbol
                )
              val valDefs =
                for (valSym, info) <- valSyms.zip(params) yield info match
                  case ParamInfo.Context(_) =>
                    ValDef(valSym, Some('{ ctx }.asTerm))
                  case arg: ParamInfo.Arg =>
                    ValDef(
                      valSym,
                      Some(initialValue(arg.symbol.termRef.widenTermRefByName))
                    )

              val paramChecks = params
                .zip(valSyms)
                .collect:
                  case (arg: ParamInfo.Arg, valSym) =>
                    val result = arg.collectionFactory match
                      case Some(factoryExpr) =>
                        factoryExpr.asTerm.tpe.asType match
                          case '[scala.collection.Factory[elem, col]] =>
                            '{
                              val api = ${ arg.api }
                              api.parseRepeatedParam(
                                flag = ${ Expr(arg.flag) },
                                args = args(${ arg.name }),
                                reader = ${ arg.reader }
                                  .asInstanceOf[api.Reader[elem]],
                                factory = ${ factoryExpr }
                                  .asInstanceOf[scala.collection.Factory[
                                    elem,
                                    col
                                  ]]
                              )
                            }
                      case None =>
                        arg.symbol.termRef.widenTermRefByName.asType match
                          case '[t] =>
                            '{
                              val api = ${ arg.api }
                              api.parseSingleParam[t](
                                flag = ${ Expr(arg.flag) },
                                name = ${ arg.name },
                                args = args(${ arg.name }),
                                reader =
                                  ${ arg.reader }.asInstanceOf[api.Reader[t]],
                                default = ${
                                  arg.default match
                                    case Some(defExpr) =>
                                      '{
                                        Some(${ defExpr }.asInstanceOf[() => t])
                                      }
                                    case None =>
                                      '{ None }
                                }
                              )
                            }
                    arg.symbol.termRef.widenTermRefByName.asType match
                      case '[t] =>
                        '{
                          ${ result.asExprOf[InvocationResult[t]] } match
                            case InvocationResult.Success(v) =>
                              ${ Assign(Ref(valSym), '{ v }.asTerm).asExpr }
                            case pe: InvocationResult.ParamError =>
                              paramErrors += pe
                            case _ =>
                        }.asTerm

              val checkAndCall = '{
                if paramErrors.nonEmpty then
                  InvocationResult.ParamError(
                    missing = paramErrors.flatMap(_.missing).toList,
                    unknown = paramErrors.flatMap(_.unknown).toList,
                    invalid = paramErrors.flatMap(_.invalid).toList
                  )
                else
                  try
                    val result = ${
                      val it = valSyms.iterator
                      val base: Term = Ref(method)
                      val args =
                        for params <- method.paramSymss
                        yield for param <- params yield Ref(it.next())
                      val application =
                        args.foldLeft(base)((lhs, args) => Apply(lhs, args))
                      application.asExprOf[retType]
                    }
                    InvocationResult.Success(result)
                  catch
                    case ex: Exception =>
                      InvocationResult.Exception(ex)
              }.asTerm

              Block(valDefs ++ paramChecks, checkAndCall)
                .asExprOf[InvocationResult[retType]]
            }
        }
        invokeFn
      case _ => sys.error("unreachable")
  end invoke

  def commandOrGroup(
      containerExpr: Expr[AnyRef],
      root: Boolean = false
  ): Expr[Command[?, ?]] =
    val container = containerExpr.asTerm

    val cmds = commandInfosFromContainer(container)
    if cmds.size == 0 then
      report.errorAndAbort(
        s"No methods annotated with @command or @group found in container ${container.show}",
        container.pos
      )

    val (terminals, nonterminals) = cmds.partition(!_.isGroup)

    if nonterminals.isEmpty then
      if terminals.size == 1 then commandInfoAsCommand(cmds.head, Nil, root)
      else
        report.errorAndAbort(
          s"More than one method annotated with @command found in container ${container.show}, but no @group method found.",
          container.pos
        )
    else
      if nonterminals.size > 1 then
        report.errorAndAbort(
          s"Implementation restriction: only one @group is allowed per container, but multiple groups found in ${container.show}. You can work around this by moving groups to separate containers.",
          container.pos
        )
      val group = nonterminals.head

      val mismatched = terminals.collect:
        case cmd if !(cmd.ctxType =:= group.returnType) => cmd

      if mismatched.nonEmpty then
        val message = "Command context type mismatch:\n" +
          s"the @group method ${group.methodSymbol.name} has a return type:\n" +
          s"  ${group.returnType.show}\n" +
          s"but the following commands have @context parameters of different types:\n"
          + mismatched
            .map(cmd =>
              s"  ${cmd.methodSymbol.name}: ${cmd.ctxType.show} != ${group.returnType.show}"
            )
            .mkString("\n")

        report.errorAndAbort(
          message,
          container.pos
        )

      val children = terminals.map(cmd => commandInfoAsCommand(cmd, Nil))
      commandInfoAsCommand(
        group,
        children,
        root
      )

  def childCommands(
      container: Expr[AnyRef]
  ): Expr[List[Command[?, ?]]] =
    val cmds = commandInfosFromContainer(container.asTerm)

    if cmds.isEmpty then '{ Nil }
    else
      val head = cmds.head
      for cmd <- cmds.tail do
        if !(cmd.ctxType =:= head.ctxType) then
          report.errorAndAbort(
            s"All @context parameters in ${container.asTerm.show} must have the same type"
          )

      head.ctxType.asType match
        case '[ctxType] =>
          Expr.ofList(
            cmds.map(cmd =>
              commandInfoAsCommand(cmd, Nil).asExprOf[Command[ctxType, ?]]
            )
          )
