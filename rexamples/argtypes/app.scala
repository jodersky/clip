// Clip supports reading many different types of parameters, including
//
// - primitive types (Int, String, Boolean, Double, etc.)
// - paths (os.Path, java.nio.file.Path, java.io.File)
// - tuples of key=value pairs
// - times and durations (java.time types, and scala.concurrent.duration types)
//
// But what if you have a custom type that you want to read as a command line
// parameter?
//
// Clip supports this via the concept of "readers". A reader is essentially a
// function that takes a `String` and produces a value of the desired type, or
// fails with an error message. In order to make your custom type usable as a
// command line parameter, you need to provide an implicit reader for it.

//snippet:start
// this is our custom type
case class LogLevel(number: Int)

// here we define the implicit reader for our custom type
given clip.Reader[LogLevel] with
  def read(s: String): clip.ReadResult[LogLevel] =
    s.toLowerCase match
      case "debug" => clip.ReadResult.Success(LogLevel(1))
      case "info"  => clip.ReadResult.Success(LogLevel(2))
      case "warn"  => clip.ReadResult.Success(LogLevel(3))
      case "error" => clip.ReadResult.Success(LogLevel(4))
      case other   => clip.ReadResult.Error(
        s"invalid log level: '$other'. Valid levels are: debug, info, warn, error"
      )

  def typeName: String = "loglevel" // used in help messages

@clip.command()
def app(level: LogLevel): Unit =
  clip.echo(s"Log level set to: ${level.number}")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

/* usage snippet
$ ./app info
Log level set to: 2
*/

/* usage snippet
$ ./app verbose
invalid value for 'level': invalid log level: 'verbose'. Valid levels are: debug, info, warn, error
run with --help for more information
*/

// If you find yourself needing to define readers for many custom types, in
// various parts of your codebase, you may want to check out the section about
// "API traits".
