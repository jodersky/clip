//snippet:start
@clip.command()
def app() =
  // The `clip.util.term` utility package provides a convenient way to access
  // terminal properties such as size and checks whether a stream is a TTY.
  //
  // It is useful for command line applications that need to adapt their output
  // based on these properties.

  // Get the size of the terminal in rows and columns. Or a default size of
  // 24x80 if the terminal size cannot be determined.
  val (rows, cols) = clip.util.term.sizeOrDefault()
  clip.echo(s"Terminal size: $rows rows, $cols columns")

  // Check if the standard streams are TTYs (i.e., if they are connected to a
  // terminal). This is more powerful than the standard library's
  // `System.console().isTerminal`, since it allows checking per stream.
  val inIsTty: Boolean  = clip.util.term.isatty(System.in)
  val outIsTty: Boolean = clip.util.term.isatty(System.out)
  val errIsTty: Boolean = clip.util.term.isatty(System.err)
  clip.echo(s"stdin is TTY: $inIsTty")
  clip.echo(s"stdout is TTY: $outIsTty")
  clip.echo(s"stderr is TTY: $errIsTty")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// ```
// $ echo "" | ./app 2> /dev/null
// Terminal size: 24 rows, 80 columns
// stdin is TTY: false
// stdout is TTY: true
// stderr is TTY: false
// ```

// > [!NOTE] The `clip.util.term` package interacts with the system's native
// > libraries (termios.h on unixy systems). It currently is only implemented
// > for Linux and macOS, but contributions for other platforms are welcome.
