package clip.dispatch

import clip.getopt

/** Represents a single command in a composable command trie.
  *
  * This is a higher-level structure than what getopt has to offer. Essentially
  * it represents a node in a trie of commands.
  *
  * @tparam Context
  *   The type of the context object that is passed to this command. A context
  *   is an arbitrary object that is passed from a parent command.
  * @tparam Return
  *   The return type of this command. If this command has children, this type
  *   should be a subtype of the context type of the child commands.
  *
  * @param name
  *   The canonical name of the command (commands can also have aliases, but
  *   this name is used to identify the command in help messages)
  * @param help
  *   A short description of the command
  * @param listChildren
  *   A function that lists all child commands of this command
  * @param getChild
  *   A function that retrieves a child command by name
  * @param params
  *   The list of parameters that this command accepts
  * @param invoke
  *   A function that invokes this command given a context and a map of parsed
  *   arguments corresponding to the command's parameters
  * @param eagerInvokes
  *   A list of functions that are called before the main invocation of this
  *   command. This can be used to implement eager parameters that implement
  *   global behavior (e.g., global flags like --help or --version). This
  *   function returns either:
  *   - an `InvocationResult[?]` if the parsing should stop after the eager
  *     invocation (e.g., to display help or version information)
  *   - `Unit` if the eager invocation does not produce a result and the normal
  *     invocation should proceed (e.g. if the eager function was used for a
  *     side-effect, such as enabling color output)
  * @param terminal
  *   Whether this command is terminal (i.e., it does not have subcommands)
  */
case class Command[-Context, Return](
    name: String,
    help: String = "",
    listChildren: () => Seq[Command[Return, ?]] = () => Seq(),
    getChild: String => Option[Command[Return, ?]] = _ => None,
    params: Seq[Param] = Seq(),
    invoke: Invoke[Context, Return],
    eagerInvokes: Seq[InvokeEager[Context]] = Seq(),
    terminal: Boolean = true
)

type Invoke[-A, +R] = (
    Seq[Command[?, ?]],
    A,
    collection.Map[String, Iterable[getopt.Arg]]
) => InvocationResult[R]

type InvokeEager[-A] = (
    Seq[Command[?, ?]],
    A,
    collection.Map[String, Iterable[getopt.Arg]]
) => EagerResult

enum EagerResult:
  case Continue // proceed with next eager or normal invocation
  case Return(
      result: InvocationResult[Any]
  ) // return this result immediately, and stop further processing

case class ParamHelp(
    names: Seq[String],
    help: String,
    repeats: Boolean,
    argName: Option[String],
    argOptional: Boolean = false
)

/** A parameter of a command.
  *
  * @param argName
  *   The name of the parameter's argument, as shown in help messages. `None`
  *   means that the parameter is a flag, i.e. it does not take an argument.
  * @param argOptional
  *   Only applicable to named parameters which have an `argName`. If true, the
  *   argument is optional and can only be given in embedded form, e.g.
  *   `--jobs` or `--jobs=4`. Since the argument is optional, `--jobs 4` is
  *   interpreted as `--jobs` without argument, followed by a separate `4`.
  */
case class Param(
    names: Seq[String],
    argName: Option[String] = Some("value"),
    endOfNamed: Boolean = false,
    repeats: Boolean = false,
    help: String = "",
    completer: clip.completion.Completer = clip.completion.Completer.Empty,
    argOptional: Boolean = false
):

  def isFlag = argName.isEmpty

  def paramDecl = clip.getopt.ParamDecl(
    name = names.head,
    aliases = names.tail,
    // getopt flags never consume the next argument, but may still have an
    // embedded argument (e.g. `--jobs=4`)
    flag = argName.isEmpty || argOptional,
    endOfNamed = endOfNamed,
    repeats = repeats
  )

  def paramHelp = ParamHelp(
    names = names,
    help = help,
    repeats = repeats,
    argName = argName,
    argOptional = argOptional
  )
