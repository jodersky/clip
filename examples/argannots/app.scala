// Instead of relying on the default parameter-to-CLI mapping, you can
// customize the command line interface using annotations.

//snippet:start
@clip.command(
  name = "foo",
  help = "A simple clip application"
)
def app(
  @clip.arg(name = "--count", aliases = Seq("-c"), help = "Number of times to repeat")
  count: Int = 5,
  @clip.arg(name = "name1", help = "First name to greet")
  name1: String,
  @clip.arg(name = "--name2", help = "Second name to greet. Notice the use of -- here, making it a named parameter.")
  name2: String
): Unit =
  println(s"count: ${count}, name1: ${name1}, name2: ${name2}")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end


/* usage snippet
$ ./app Alice -c 3 --name2 Bob
count: 3, name1: Alice, name2: Bob
*/

/* usage snippet
$ ./app Alice
missing required argument: '--name2'
run with --help for more information
*/

/* usage snippet
$ ./app --help
Usage: foo [options] name1

A simple clip application

Options:
  --color <mode>         Set color mode (auto, always, never)
  --completion <shell>   Generate shell completion script
  -c, --count <integer>  Number of times to repeat
  --help                 Show help message
  --name2 <string>       Second name to greet. Notice the use of -- here, making
                          it a named parameter.
Positional arguments:
  name1                  First name to greet
*/

// Notice the that the help message reflects the custom names and descriptions.
// It also includes some additional options which we have not defined ourselves,
// such as `--color` and `--completion`. We'll get into these a bit later.
