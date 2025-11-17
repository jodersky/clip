import scala.scalanative.posix.sys.un
@clip.command()
def app() =

  // You can use clip's `echo` function for styling terminal output. It's like
  // println, but is aware of terminal capabilities and strips colors if not
  // running in a terminal.
  clip.echo(clip.style(fg = clip.Green, bold = true, underline = true)("Hello, World!"))

  // You can also prompt the user for input. Here we ask for an integer.
  // When running in a non-interactive terminal, the default value will be used.
  clip.prompt[Int]("Enter a number", default = 0) match
    case 42 =>
      clip.echo(clip.style(fg = clip.Green)("You found the meaning of life!"))
    case value: Int =>
      clip.echo(s"You entered: ${clip.style(fg = clip.Cyan)(value.toString)}")

  // You can also pause execution until the user presses Enter.
  // Again, in a non-interactive terminal this will be skipped.
  clip.pause("We're about to show progress bars. Press Enter to continue.")

  val upper = clip.prompt[Int]("Enter a number", default = 1000)

  // Progress bars can be used to wrap any iterable. Here we show a simple
  // progress bar counting to the number entered by the user.
  for item <- clip.progressbar("Processing").wrap(1 to upper) do
    // simulate work
    Thread.sleep(10)

  // You can also wrap `geny.Readables`, e.g., file streams.
  val data: geny.Readable = "hello world"
  val progress = clip.progressbar("Reading bytes").wrap(data)

  progress.readBytesThrough(stream =>
    while stream.read() != -1 do
      // simulate work
      Thread.sleep(10)
  )

def main(args: Array[String]) = clip.main(this, args)
