//snippet:start
@clip.command()
def app() =

  // `clip.echo` is similar to println, but is aware of whether the output
  // is being redirected to a file or a pipe, and strips ANSI escape codes
  // (like colors) in that case
  clip.echo("Hello, World!")

  // this line has red text when printed to a terminal, and normal text
  // when redirected to a file or pipe
  clip.echo("This is a line with \u001b[31mred\u001b[0m text.")

  // often times in CLI applications you want to print tabular data
  // `clip.tabulate` helps with that by aligning the columns nicely
  clip.tabulate(
    Seq("Name", "Age", "City"),
    Seq(
      Seq("Alice", "30", "New York"),
      Seq("Bob", "25", "Los Angeles"),
      Seq("Charlie", "35", "Chicago"),
    )
  )

  // use `clip.log` to print diagnostic information to stderr
  clip.log("This is a log message that goes to stderr.")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end
//
// Example usage:
/* usage snippet
$ ./app
Hello, World!
This is a line with red text.
Name      Age   City
Alice     30    New York
Bob       25    Los Angeles
Charlie   35    Chicago
This is a log message that goes to stderr.
*/

// Clip's output utilities handle ANSI escape codes in a sane way. They are
// stripped or kept based on the following rules:
//
// 1. check if the command line parameter `--color` is provided:
//    - if `--color=always`, ANSI codes are always kept
//    - if `--color=never`, ANSI codes are always stripped
//    - if `--color=auto` or not provided, proceed to the next step
//
// 2. check the environment:
//    - if FORCE_COLOR is set, ANSI codes are always kept
//    - if NO_COLOR is set or TERM is dumb, ANSI codes are always stripped
//    - if neither is set, proceed to the next step
//
// 3. check the output destination:
//    - if output is a terminal, ANSI codes are kept
//    - if output is a file or a pipe, ANSI codes are stripped
//
// Essentially, this means that unless you explicitly forbid or force colors via
// the command line or environment variables, colors will be kept when printing
// to a terminal, and stripped when redirecting output to a file or a pipe.

/* usage snippet
$ ./app --color=always
Hello, World!
This is a line with [31mred[0m text.
Name      Age   City
Alice     30    New York
Bob       25    Los Angeles
Charlie   35    Chicago
This is a log message that goes to stderr.
*/
