package clip.dispatch

import scala.collection.mutable.ListBuffer

enum InvocationResult[+A]:
  // case ParseError // something went wrong parsing
  // case CommandError

  /** The command was invoked successfully
    * @param value
    *   Whatever the command returned (usually not useful)
    */
  case Success(value: A)

  /** There was an error parsing arguments
    *
    * @param missing
    *   A list of names of parameters that were missing
    * @param unknown
    *   A list of arguments that were unknown
    * @param invalid
    *   A list of pairs of parameter names and reasons why they were invalid
    */
  case ParamError(
      missing: Seq[String] = Seq(),
      unknown: Seq[String] = Seq(),
      invalid: Seq[(String, String)] = Seq()
  ) extends InvocationResult[Nothing]

  /** Invoking the command failed with an exception */
  case Exception(t: Throwable)

  /** A subcommand was expected but not specified */
  case MissingCommand()

  /** An unknown subcommand was specified
    *
    * @param commandName
    *   The name of the unknown command
    */
  case UnknownCommand(commandName: String)

  def isSuccess = this.isInstanceOf[Success[?]]

/** Call a command with the given arguments, printing errors to the given output
  * streams.
  *
  * @return
  *   exit code: 0 on success, 2 on argument errors, fall through on exceptions
  */
def invoke(
    command: Command[Unit, ?],
    args: Iterable[String],
    stderr: java.io.PrintStream = System.err
): Int =
  val (chain, result) = invokeRaw(command, (), args, stderr)
  val c = Colorizer(stderr)
  def hint() =
    if chain.last.params.exists(_.names.head == "--help") then
      stderr.println(s"run with ${c.literal("--help")} for more information")

  result match
    case InvocationResult.Success(_) =>
      0
    case InvocationResult.ParamError(missing, unknown, invalid) =>
      if unknown.nonEmpty then
        stderr.println(
          s"${c.error("unknown argument")}: ${c.quoted(unknown.head)}"
        )
      else
        for name <- missing do
          stderr.println(
            s"${c.error("missing required argument")}: ${c.quoted(name)}"
          )
        for (name, reason) <- invalid do
          stderr.println(
            s"${c.error("invalid value")} for ${c.quoted(name)}: $reason"
          )
      hint()
      2
    case InvocationResult.MissingCommand() =>
      stderr.println(
        s"${c.error("missing subcommand")} for command ${c.quoted(chain.last.name)}"
      )
      hint()
      2
    case InvocationResult.UnknownCommand(commandName) =>
      stderr.println(s"${c.error("unknown command")}: ${c.quoted(commandName)}")
      hint()
      val similar = clip.util.text.jaroWinklerClosest(
        commandName,
        chain.last.listChildren().map(_.name)
      )
      if similar.nonEmpty then
        stderr.println(c.header("similar commands:"))
        for cmd <- similar do stderr.println(s"  ${c.literal(cmd)}")
      2
    case InvocationResult.Exception(clip.util.AbortException(message, code)) =>
      stderr.println(message)
      stderr.println("Aborted")
      code
    case InvocationResult.Exception(t) =>
      throw t

/** Traverse the command trie, invoking commands as we go down.
  *
  * @return
  *   (the final command invoked, the invocation result) by final command, we
  *   mean either the first which returned a non-success result, or the terminal
  *   command at the end of the chain
  */
def invokeRaw[Ctx](
    command: Command[Ctx, ?],
    context: Ctx,
    args: Iterable[String],
    stderr: java.io.PrintStream = System.err
): (Iterable[Command[?, ?]], InvocationResult[?]) =
  val completing = sys.env.get("COMPLETE_LAST").isDefined

  val it = args.iterator
  val chain = ListBuffer.empty[Command[?, ?]]
  var current: Command[Any, ?] = command.asInstanceOf[Command[Any, ?]]
  var ctx: Any = context

  while true do
    chain += current

    val paramDecls = current.params.map(_.paramDecl)
    val result = clip.getopt.parse(paramDecls, it)

    // we're completing, and there's no further command to complete, which
    // means that the parameter to complete is in the current command
    if completing && result.remainder.isEmpty then
      val paramName = result.parsed.last._1
      val param = current.params.find(_.names.contains(paramName)).get
      val partial = result.parsed.last._2.value.getOrElse("")

      param.completer match
        case clip.completion.Completer.Dynamic(f) =>
          val candidates =
            f.asInstanceOf[(Any, String) => Set[String]](ctx, partial)
          System.out.println(candidates.mkString("\n"))
          return (chain, InvocationResult.Success(()))
        case _ =>
          // other completers should have been handled by the bash completion scripts themselves
          return (chain, InvocationResult.Success(()))

    val eagerInvokes = current.eagerInvokes.iterator
    while eagerInvokes.hasNext do
      eagerInvokes.next()(chain.toSeq, ctx, result.grouped) match
        case EagerResult.Return(r) =>
          return (chain, r)
        case EagerResult.Continue =>

    if current.terminal then
      result.remainder match
        case Some((next, _)) =>
          return (chain, InvocationResult.ParamError(unknown = Seq(next)))
        case None =>
          return (chain, current.invoke(chain.toSeq, ctx, result.grouped))
    else
      current
        .asInstanceOf[Command[Any, ?]]
        .invoke(chain.toSeq, ctx, result.grouped) match
        case InvocationResult.Success(v) =>
          ctx = v
        case err =>
          return (chain, err)

      result.remainder match
        case Some((next, args)) =>
          current.getChild(next) match
            case Some(child) =>
              current = child.asInstanceOf[Command[Any, ?]]
            case None =>
              return (chain, InvocationResult.UnknownCommand(next))
        case None =>
          return (chain, InvocationResult.MissingCommand())

  sys.error("unreachable")
end invokeRaw

/** Styles used for help and error messages. All styling is skipped if the
  * target stream should not be colored (see [[clip.util.termui.ColorMode]]),
  * in which case the output is plain text.
  */
private class Colorizer(out: java.io.PrintStream):
  val enabled: Boolean = clip.util.termui.ColorMode.global.useColor(out)

  // SGR codes are emitted directly (rather than via termui.style) to keep the
  // output compact
  private def sgr(codes: String)(s: String): String =
    if enabled && s.nonEmpty then s"\u001B[${codes}m$s\u001B[0m" else s

  def header(s: String) = sgr("33")(s) // yellow
  def literal(s: String) = sgr("36")(s) // cyan
  def placeholder(s: String) = sgr("32")(s) // green
  def error(s: String) = sgr("1;31")(s) // bold, red
  def quoted(s: String) = sgr("33")(s"'$s'") // yellow

def usage2(
    out: java.io.PrintStream,
    command: Command[?, ?],
    chain: Iterable[String]
) =
  val c = Colorizer(out)
  out.print(c.header("Usage:"))
  out.print(" ")
  out.print(c.literal(chain.mkString(" ")))

  val (named, positional) =
    command.params.map(_.paramHelp).partition(_.names.head.startsWith("-"))

  if named.nonEmpty then out.print(" " + c.placeholder("[options]"))
  if positional.nonEmpty then
    for param <- positional do
      out.print(" ")
      out.print(c.placeholder(param.names.head))
      if param.repeats then out.print(c.placeholder("..."))

  if !command.terminal then
    out.print(" " + c.placeholder("<command> [<args>...]"))

  out.println()

/** A left-hand column entry: its plain text (used for layout) and its styled
  * rendering (which may contain ANSI escapes).
  */
private case class Lhs(plain: String, styled: String)

private def sideBySide(
    col1: Seq[Lhs],
    col2: Seq[String],
    col1Width: Int,
    termWidth: Int,
    out: java.io.PrintStream
): Unit =
  val col2Width = termWidth - col1Width - 2

  if col2Width < 50 then
    // not enough space for two columns, print differently
    for (lhs, rhs) <- col1.zip(col2) do
      out.println(lhs.styled)
      val lines = clip.util.text.WrappedLines(
        rhs.iterator,
        termWidth - 4
      )
      for line <- lines do
        out.print("    ")
        out.println(line)
      out.println()
  else
    // the left column is at most col1Width wide by construction, so it never
    // needs wrapping; only the right column is wrapped
    for (lhs, rhs) <- col1.zip(col2) do
      val lines = clip.util.text.WrappedLines(rhs.iterator, col2Width)
      out.print(lhs.styled)
      if lines.hasNext then
        out.print(" " * (col1Width - lhs.plain.length))
        out.print("  ")
        out.println(lines.next())
        for line <- lines do
          out.print(" " * (col1Width + 2))
          out.println(line)
      else out.println()

/** Generate a help message for the given command.
  *
  * The help message looks like this:
  *
  * ```
  * usage: root sub sub [options] named
  *
  * <command help>
  *
  * options:
  *   -h, --help        Show help message
  *   --version         Show version information
  * positional arguments:
  *   input             Input file
  *   output            Output file
  * commands:
  *   add               Add something
  *   remove            Remove something
  * ```
  *
  * The message is colored if the output stream supports it and the global
  * color mode allows it.
  */
def help2(
    out: java.io.PrintStream,
    command: Command[?, ?],
    chain: Iterable[String]
): Unit =
  val c = Colorizer(out)
  usage2(out, command, chain)
  out.println()

  if !command.help.isEmpty() then
    out.println(command.help)
    out.println()

  val termWidth = clip.util.term.sizeOrDefault()._2
  val (named0, positional) =
    command.params.map(_.paramHelp).partition(_.names.head.startsWith("-"))
  val named = named0.sortBy(_.names.head)

  // -short, --long tpe
  val namedLhs = for param <- named yield
    val long = param.names.head
    val short =
      if long.length == 2 then ""
      else param.names.find(_.length == 2).getOrElse("")
    val argname = param.argName.map(n => s"<$n>").getOrElse("")

    if short != "" then
      Lhs(
        s"  $short, $long $argname",
        s"  ${c.literal(short)}, ${c.literal(long)} ${c.placeholder(argname)}"
      )
    else
      Lhs(
        s"  $long $argname",
        s"  ${c.literal(long)} ${c.placeholder(argname)}"
      )

  val positionalLhs = for param <- positional yield
    Lhs(s"  ${param.names.head}", s"  ${c.placeholder(param.names.head)}")

  val commandLhs = for cmd <- command.listChildren() yield
    Lhs(s"  ${cmd.name}", s"  ${c.literal(cmd.name)}")

  var col1Width = 0
  for l <- namedLhs ++ positionalLhs ++ commandLhs do
    if l.plain.length > col1Width then col1Width = l.plain.length

  if named.nonEmpty then
    out.println(c.header("Options:"))
    sideBySide(namedLhs, named.map(_.help), col1Width, termWidth, out)

  if positional.nonEmpty then
    out.println(c.header("Positional arguments:"))
    sideBySide(positionalLhs, positional.map(_.help), col1Width, termWidth, out)

  if !command.terminal then
    val children = command.listChildren()
    if children.nonEmpty then
      out.println(c.header("Commands:"))
      sideBySide(commandLhs, children.map(_.help), col1Width, termWidth, out)

/** Generate a bash completion script for a command and its children
  * recursively.
  */
def generateCompletionRecursively(
    names: Seq[String],
    out: java.io.PrintStream,
    cmd: Command[?, ?]
): Unit =
  import scala.collection.mutable as m
  import clip.completion as c

  val stack = m.Stack.empty[(Seq[String], Command[?, ?])]
  stack.push(Seq(cmd.name) -> cmd)

  out.println(c.BashCompletion.doc(cmd.name))
  out.println(c.BashCompletion.utils(cmd.name))

  while stack.nonEmpty do
    val (chain, current) = stack.pop()

    for child <- current.listChildren() do
      stack.push((chain :+ child.name) -> child)

    out.println(
      c.BashCompletion.specs(
        chain = chain,
        params = current.params.map: p =>
          c.ParamInfo(
            names = p.names,
            isFlag = p.isFlag,
            argName = p.argName,
            repeats = p.repeats,
            completer = p.completer,
            help = p.help
          ),
        subcommands = current
          .listChildren()
          .map: s =>
            c.SubcommandInfo(
              name = s.name,
              help = s.help
            )
      )
    )
  end while

  out.println(c.BashCompletion.hook(cmd.name, names))
