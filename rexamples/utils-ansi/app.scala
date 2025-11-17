//snippet:start
@clip.command()
def app() =

  // Use `clip.style` to create colored and styled text
  println(clip.style(fg=clip.Green)("Hello, colored world!"))

  // You can nest styles for more complex formatting, where inner styles
  // override outer styles as needed. The API is similar to scalatags.
  clip.echo(
    clip.style(fg=clip.Red, italic=true, bold = true)(
        "Hello, ",
        clip.style(fg = clip.Rgb(0, 20, 30), italic = false, underline=true)(
          "ne",
          clip.style(fg=clip.Blue)("st"),
          "ed"
        ),
        " world!",
    ).render()
  )

  val parts = for i <- 0 until 256 yield clip.style(bg = clip.Rgb(10, 255-i, i))(" ").render()
  for line <- parts.grouped(32).map(_.mkString("")) do
    clip.echo(line)

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// ![output](rexamples/utils-ansi/out.png)

// While convenient, `clip.style` may not be the most efficient if you have
// deeply nested styles. If it becomes a performance bottleneck, consider
// using dedicated library such as
// [fansi](https://github.com/com-lihaoyi/fansi).
