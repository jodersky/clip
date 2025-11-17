@clip.command(
  help = "This is an example app. It shows how a command line interface can be " +
  "generated from various kinds of method parameters."
)
def main(
  @clip.arg("--server", help = "a sample named parameter")
  server: String = "localhost",
  @clip.arg("--secure", help = "this is a flag")
  secure: Boolean = false,
  @clip.arg("path", help = "a positional parameter")
  path: os.SubPath
) =
  val scheme = if secure then "https" else "http"
  clip.echo(s"$scheme://$server/$path")

def main(args: Array[String]): Unit = clip.main(this, args)
