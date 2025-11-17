// Here is what it looks like:
//snippet:start
//> using dep io.crashbox::clip::0.1.0

@clip.command(help = "Simple program that greets NAME for a total of COUNT times.")
def hello(
    @clip.arg("--count", help = "Number of greetings")
    count: Int = 3,
    @clip.arg("name",  help ="Your name")
    name: String
): Unit =
    for _ <- 0 until count do
        clip.echo(s"Hello ${name}!")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// When run:

/* usage snippet
$ ./app --count=3 John
Hello John!
Hello John!
Hello John!
*/

// Automatically generated help message (nicely laid out according to your
// current terminal size):

/* usage snippet
$ ./app --help
Usage: hello [options] name

Simple program that greets NAME for a total of COUNT times.

Options:
  --color <mode>        Set color mode (auto, always, never)
  --completion <shell>  Generate shell completion script
  --count <integer>     Number of greetings
  --help                Show help message
Positional arguments:
  name                  Your name
*/

// In case of errors, it prints a summary of what is wrong:

/* usage snippet
$ ./app --count=oops
missing required argument: 'name'
invalid value for '--count': 'oops' is not an integral number
run with --help for more information
*/
