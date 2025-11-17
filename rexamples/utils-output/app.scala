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
*/

/* usage snippet
$ ./app --color=always
Hello, World!
This is a line with [31mred[0m text.
Name      Age   City
Alice     30    New York
Bob       25    Los Angeles
Charlie   35    Chicago
*/
