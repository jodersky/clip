package clip.completion

case class ParamInfo(
    names: Seq[String],
    isFlag: Boolean,
    argName: Option[String],
    repeats: Boolean,
    completer: Completer,
    help: String
):
  def isNamed = names.head.startsWith("-")

case class SubcommandInfo(
    name: String,
    help: String
)
