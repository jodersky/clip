package clip.completion

case class ParamInfo(
    names: Seq[String],
    isFlag: Boolean,
    repeats: Boolean,
    completer: Completer
):
  def isNamed = names.head.startsWith("-")
