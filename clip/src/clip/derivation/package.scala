package clip.derivation

import clip.dispatch.Command
import clip.dispatch.InvocationResult
import clip.dispatch.InvokeEager
import scala.annotation.StaticAnnotation
import clip.derivation.ReadResult

/** Annotate a function with `@command()` to make it callable from the command
  * line.
  * @param name
  *   The name of the command. If not provided, the method name is used.
  * @param help
  *   A short description of the command, used in help messages
  * @param version
  *   An optional version string for the command
  */
class command(
    name: String = null,
    help: String = "",
    version: String = null
) extends StaticAnnotation

/** A group can be used to pass a context into other commands
  *
  * @param name
  *   The name of the group. If not provided, the method name is used.
  * @param help
  *   A short description of the group, used in help messages
  * @param version
  *   An optional version string for the group
  * @param subcommands
  *   An optional list of subcommands to add to this group
  */
class group(
    name: String = null,
    help: String = "",
    version: String = null,
    subcommands: Seq[Command[?, ?]] = Seq()
) extends StaticAnnotation

/** Annotate a parameter with `@context()` to indicate that its value should be
  * provided from the command's parent group.
  */
class context() extends StaticAnnotation

/** Create commands for all methods annotated with `@command`/`@group` in the
  * given container object.
  *
  * Note that this will return a collection of all commands, not a single
  * command that you can run. Use `commandFor` or `main` to get a top-level
  * command. This is mainly useful for adding child commands to a parent
  * manually in complex CLI applications.
  *
  * @see
  *   clip.derivation.commandFor
  *
  * @param container
  *   An object containing methods annotated with `@command`/`@group`
  * @return
  *   A list of commands derived from the container object
  */
inline transparent def subcommandsFor(
    container: AnyRef
): List[Command[?, ?]] =
  ${ subcommandsForImpl('container) }

// all our macros are contained in the `macros` helper class, but we can't
// instantiate that directly from inline methods, so we use a "telescopic"
// function instead, which simply forwards to the actual macro implementation
private def subcommandsForImpl(using
    qctx: scala.quoted.Quotes
)(container: scala.quoted.Expr[AnyRef]) =
  macros().childCommands(container)

/** Create a command for a container object. The container object must have
  * either:
  *
  *   1. a single method annotated with `@command`/`@group`, or
  *   2. a single method annotated with `@group` and multiple methods annotated
  *      with `@command`
  *
  * In case of a single `@command`/`@group`, that method is used to generate the
  * command. In case of a `@group` with multiple `@command`s, the group is used
  * as the top-level command, and the commands are added as children
  * automatically (in addition to any explicitly added subcommands).
  */
inline transparent def commandFor(
    container: AnyRef
): Command[?, ?] =
  ${ commandForImpl('container, 'false) }

inline transparent def commandFor(
    container: AnyRef,
    inline root: Boolean
): Command[?, ?] =
  ${ commandForImpl('container, 'root) }

// all our macros are contained in the `macros` helper class, but we can't
// instantiate that directly from inline methods, so we use a "telescopic"
// function instead, which simply forwards to the actual macro implementation
private def commandForImpl(using
    qctx: scala.quoted.Quotes
)(container: scala.quoted.Expr[AnyRef], root: scala.quoted.Expr[Boolean]) =
  macros().commandOrGroup(container, root.valueOrAbort)

/** Create a top-level command from a container object and invoke it with the
  * given arguments.
  *
  * This is similar to calling `commandFor` and then invoking the resulting
  * command. It is intended to be used as an application's `main` method.
  *
  * ```
  * def main(args: Array[String]): Unit = clip.main(this, args)
  * ```
  */
inline def main(container: AnyRef, args: Array[String]): Unit = ${
  mainImpl('container, 'args)
}

private def mainImpl(using
    qctx: scala.quoted.Quotes
)(
    containerExpr: scala.quoted.Expr[AnyRef],
    argsExpr: scala.quoted.Expr[Array[String]]
) =
  import qctx.reflect.*

  macros().commandOrGroup(containerExpr, true) match
    case '{ $cmd: Command[Unit, ?] } =>
      '{
        val code = clip.dispatch.invoke($cmd, $argsExpr.toSeq)
        sys.exit(code)
      }
    case _ =>
      report.errorAndAbort(
        s"container must be a command or group that takes Unit context"
      )

/** An API trait is used to contain "globally configurable" aspects of the
  * command derivation system. It is only intended to be extended by advanced
  * users who want to customize the behavior of the derivation system.
  *
  * For example, it is used to make readers path-dependent on `arg` annotations,
  * which in turn can then be extended by users to define their own readers for
  * given parameter types globally.
  */
trait Api extends ReaderApi with clip.completion.CompletionApi:

  given readerFromString[A](using fs: clip.readers.FromString[A]): Reader[A]
  with
    def read(str: String): ReadResult[A] =
      fs.fromString(str) match
        case Left(msg)    => ReadResult.Error(msg)
        case Right(value) => ReadResult.Success(value)
    def typeName: String = fs.typeName

  /** Annotate a parameter with @arg to indicate that its value should be
    * provided by a command-line argument.
    *
    * @param name
    *   The name of the argument (e.g., "--foo" or "-f", "foo", etc.). Note that
    *   the name influences how the argument is parsed from the command line:
    *   - if the name starts with "--" or "-", it is treated as a named argument
    *     (that can occur in any order)
    *   - otherwise, it is treated as a positional argument (the order matters)
    * @param aliases
    *   Alternative names for the argument (e.g., "-f" for "--foo")
    * @param description
    *   A short description of the argument, used in help messages
    * @param repeats
    *   Whether the argument can be repeated multiple times to collect multiple
    *   values (e.g., `--tag foo --tag bar`)
    * @param reader
    *   An optional custom reader for this argument. If not provided, a default
    *   reader will be used.
    * @param completer
    *   An optional custom completion function for this argument. If not
    *   provided, a default completion function based on the argument type will
    *   be used.
    */
  case class arg(
      name: String,
      aliases: Seq[String] = Seq(),
      help: String = "",
      repeats: Boolean = false,
      reader: Reader[?] = null, // override reader
      completer: clip.completion.Completer = null // override completer
  ) extends StaticAnnotation

  def parseSingleParam[P](
      flag: Boolean,
      name: String,
      args: Iterable[clip.getopt.Arg],
      reader: Reader[P],
      default: Option[() => P]
  ): InvocationResult[P] =
    def read(nameUsed: String, argOpt: Option[String]): InvocationResult[P] =
      val arg: String = argOpt match
        case Some(s)      => s
        case None if flag => "true"
        case None         =>
          return InvocationResult.ParamError(invalid =
            Seq((nameUsed, "argument expected"))
          )
      reader.read(arg) match
        case ReadResult.Error(msg) =>
          InvocationResult.ParamError(invalid = Seq((nameUsed, msg)))
        case ReadResult.Success(a) =>
          InvocationResult.Success(a)

    if args.isEmpty && default.isDefined then
      InvocationResult.Success(default.get())
    else if args.isEmpty then InvocationResult.ParamError(missing = Seq(name))
    else if args.size > 1 then
      InvocationResult.ParamError(invalid = Seq((name, "too many arguments")))
    else
      val arg = args.head
      read(arg.param, arg.value)

  def parseRepeatedParam[Elem, Col](
      flag: Boolean,
      args: Iterable[clip.getopt.Arg],
      reader: Reader[Elem],
      factory: scala.collection.Factory[Elem, Col]
  ): InvocationResult[Col] =
    val builder = factory.newBuilder

    val argsIt = args.iterator
    while argsIt.hasNext do
      val arg = argsIt.next()
      val nameUsed = arg.param
      val valueOpt = arg.value

      val result = valueOpt match
        case Some(value) =>
          reader.read(value)
        case None if flag =>
          reader.read("true")
        case None =>
          return InvocationResult.ParamError(invalid =
            Seq((nameUsed, "argument expected"))
          )

      result match
        case ReadResult.Error(msg) =>
          return InvocationResult.ParamError(invalid = Seq((nameUsed, msg)))
        case ReadResult.Success(v) =>
          builder += v

    InvocationResult.Success(builder.result())

  // TODO: should this really be in clip.derivation.Api?
  def prompt[A](
      message: String,
      default: A | Null = null,
      password: Boolean = false,
      showDefault: Boolean = true,
      defaultString: A => String = (a: A) => a.toString
  )(using reader: Reader[A]): A =
    if clip.util.term.isatty(0) then
      while true do
        val msg =
          if showDefault && default != null then
            s"$message [default: ${defaultString(default.asInstanceOf[A])}]"
          else message

        val input = if password then
          clip.util.termui.echo(msg + " (input hidden): ", nl = false)
          val r = clip.util.term.readHidden()
          clip.util.termui.echo("") // newline after hidden input
          r
        else
          clip.util.termui.echo(msg + ": ", nl = false)
          scala.io.StdIn.readLine()

        if (input == null || input.isEmpty) && default != null then
          return default.asInstanceOf[A]
        else if input == null then clip.util.abort(2, "No input provided")

        reader.read(input) match
          case ReadResult.Error(msg) =>
            clip.util.termui.echo(s"Invalid input: $msg")
          case ReadResult.Success(v) =>
            return v

      sys.error("unreachable")
    else if default != null then
      // not a tty, but we have a default, so just use that
      return default.asInstanceOf[A]
    else
      clip.util.abort(
        2,
        "Cannot prompt for input in non-interactive terminal (no default provided)"
      )

  def confirm(
      message: String,
      default: Boolean = false
  ): Boolean =
    if clip.util.term.isatty(0) then
      val defaultStr = if default then "Y/n" else "y/N"
      while true do
        clip.util.termui.echo(s"$message [$defaultStr]: ", nl = false)
        val input = scala.io.StdIn.readLine()

        if input == null || input.isEmpty then return default
        else
          input.toLowerCase() match
            case "y" | "yes" => return true
            case "n" | "no"  => return false
            case _           =>
              clip.util.termui.echo(
                s"Invalid input: please enter 'y' or 'n'",
                err = true
              )

      sys.error("unreachable")
    else
      // not a tty, just use the default
      default

object default extends Api
