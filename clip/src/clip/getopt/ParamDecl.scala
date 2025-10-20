package clip.getopt

case class ParamDecl(
    name: String,
    aliases: Seq[String] = Seq(),
    flag: Boolean = false,
    endOfNamed: Boolean = false,
    repeats: Boolean = false
):
  val isNamed = name.startsWith("-")
  def allNames = Seq(name) ++ aliases
