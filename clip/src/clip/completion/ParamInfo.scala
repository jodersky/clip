package clip.completion

case class ParamInfo(
    names: Seq[String],
    isFlag: Boolean,
    repeats: Boolean,
    completer: Completer,
    help: String,
    argOptional: Boolean = false
):
  def isNamed = names.head.startsWith("-")

case class SubcommandInfo(
    name: String,
    help: String
)
