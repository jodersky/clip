//> using dep io.crashbox::clip::0.1.0


@clip.command(
  name = "app",
  help = "A simple example application using clip"
)
def app(
  @clip.arg("--string-param", help = "string parameter has no completion")
  stringParam: String = "default",
  @clip.arg("--path-param", help = "paths can be completed with the filesystem")
  pathParam: os.Path = os.pwd / "default.txt",
  @clip.arg(
    "--custom-param", help = "custom parameter with dynamic completion",
    completer = clip.Completer.Dynamic(
      (_, partial) =>
        Set("foo", "bar", "baz").filter(_.startsWith(partial))
    )
  )
  customParam: String = "default"
) =
  clip.echo(s"String parameter: $stringParam")
  clip.echo(s"Path parameter: $pathParam")
  clip.echo(s"Custom parameter: $customParam")

def main(args: Array[String]): Unit = clip.main(this, args)


// scala-cli --power package -o app --native rexamples/completion.scala --
// source
// path

/* usage
$ scala app.scala --completion > app-completion.sh
$ source app-completion.sh

*/
