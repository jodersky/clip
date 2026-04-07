// If you want to return early from a command, you can call `clip.abort()`. This
// throws a `clip.AbortException`, which is caught by `clip.main` and causes it
// to exit the program.
//
// You can specify an exit code and a message to be printed.

//snippet:start
@clip.command()
def app(x: Int) =
  if x % 2 != 0 then
    clip.abort("x must be even", 100)

  clip.echo(s"x is $x")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

/* usage snippet
$ ./app 1
x must be even
Aborted
*/

// (exit code 100)

