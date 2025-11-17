// Clip supports reading many different types of parameters, including
//
// - primitive types (Int, String, Boolean, Double, etc.)
// - paths (os.Path, java.nio.file.Path, java.io.File)
// - tuples of key=value pairs
// - times and durations (java.time types, and scala.concurrent.duration types)
//
//snippet:start
@clip.command()
def main(
  num: Int = 0,
  num2: Double = 0,
  path: os.Path = os.pwd, // relative paths on the command line will be resolved to absolute paths w.r.t. to pwd
  keyValue: (String, Int) = ("a" -> 2),
  @clip.arg(name = "--key-values", repeats = true) keyValues: Seq[(String, Int)] = Seq(),
  @clip.arg(name = "--key-values-map", repeats = true) keyValuesMap: Map[String, Int] = Map(),
  duration: scala.concurrent.duration.Duration
) =
  println(s"num=$num")
  println(s"num2=$num2")
  println(s"path=$path")
  println(s"keyValue=$keyValue")
  println(s"keyValues=$keyValues")
  println(s"keyValuesMap=$keyValuesMap")
  println(s"duration=$duration")

def main(args: Array[String]) = clip.main(this, args)
//snippet:end

/* usage snippet
$ ./app \
  --num 42 \
  --num2 3.14 \
  --path /tmp \
  --key-value a=1 \
  --key-values b=2 \
  --key-values c=3 \
  --key-values-map d=4 \
  --key-values-map e=5 \
  5s
num=42
num2=3.14
path=/tmp
keyValue=(a,1)
keyValues=List((b,2), (c,3))
keyValuesMap=Map(d -> 4, e -> 5)
duration=5 seconds
*/
