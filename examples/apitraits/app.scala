// Earlier we saw how to create custom parameter readers for new types. But what
// if you want to create a reader for a type that a) you don't control, and b)
// you don't want to have `import givens` all over your code? Or what if you
// want to change the way a built-in type is read? This is where API traits come
// in.
//
// You can think of API traits as a way to "globally" configure what readers are
// to be used throughout your application, without having to import them
// everywhere.
//
// The way it works is the following:
// 1. you define a custom object that extends the configuration trait,
//    `clip.derivation.Api`
// 2. you define the readers you want to use in that object
// 3. you use that object instead of `clip` in your argument annotations
//
// Here is an example of how to use API traits to change the path reader to only
// accept absolute paths (starting with a /).
//
//snippet:start
// this is the object that implements the API trait, you'd typically define this
// once for your whole application
object mycli extends clip.derivation.Api:

  // here we override the path reader to only accept absolute paths (the default
  // reader accepts both absolute and relative paths, and converts them to
  // absolute paths relative to the current working directory)
  given Reader[os.Path] with
    def read(s: String) =
      os.FilePath(s) match
        case p: os.Path => clip.ReadResult.Success(p)
        case _          => clip.ReadResult.Error(s"$s is not an absolute path")
    def typeName = "absolute path"

@clip.command()
def app(
  @mycli.arg("path") // instead of @clip.arg, we use @mycli.arg to use our custom reader
  path: os.Path
) =
  clip.echo(s"The provided path is: $path")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

/* usage snippet
$ ./app foo/bar
invalid value for 'path': foo/bar is not an absolute path
run with --help for more information
*/

/* usage snippet
$ ./app /foo/bar
The provided path is: /foo/bar
*/
