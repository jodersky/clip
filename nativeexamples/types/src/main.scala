@clip.command()
def main(
  num: Int = 0,
  num2: Double = 0,
  path: os.Path = os.pwd, // relative paths on the command line will be resolved to absolute paths w.r.t. to pwd
  keyValue: (String, Int) = ("a" -> 2),
  @clip.arg(name = "--key-values", repeats = true) keyValues: Seq[(String, Int)] = Seq(),
  @clip.arg(name = "--key-values-map", repeats = true) keyValuesMap: Map[String, Int] = Map()
) =
  println(s"num=$num")
  println(s"num2=$num2")
  println(s"path=$path")
  println(s"keyValue=$keyValue")
  println(s"keyValues=$keyValues")
  println(s"keyValuesMap=$keyValuesMap")

def main(args: Array[String]) = clip.main(this, args)
