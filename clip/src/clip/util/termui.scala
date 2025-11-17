package clip.util.termui

enum ColorMode:
  case Always
  case Never
  case Auto

  def orElse(other: ColorMode): ColorMode =
    this match
      case ColorMode.Auto => other
      case _              => this

  def useColor(stream: java.io.PrintStream): Boolean =
    this match
      case ColorMode.Always => true
      case ColorMode.Never  => false
      case ColorMode.Auto   => clip.util.term.isatty(stream)

object ColorMode:

  // https://no-color.org/
  val globalNoColor: Boolean =
    sys.env.get("NO_COLOR") match
      case Some(str) if str.nonEmpty => true
      case _                         => false

  val globalForceColor: Boolean =
    sys.env.get("FORCE_COLOR") match
      case Some(_) => true
      case None    => false

  private val _curr = InheritableThreadLocal[ColorMode]()
  _curr.set(
    if globalNoColor then ColorMode.Never
    else if globalForceColor then ColorMode.Always
    else ColorMode.Auto
  )

  /** Set the global color mode.
    *
    * Note that this should be called early in the program execution, typically
    * right after parsing command-line options. The global color mode is stored
    * in an inheritable thread-local variable, so it will be visible to all
    * threads created from the current thread. Setting it to something else once
    * the program has spawned other threads or started outputting text may lead
    * to inconsistent behavior.
    */
  def setGlobalColorMode(mode: ColorMode): Unit = _curr.set(mode)

  /** Get the global color mode, as set by:
    *   - The NO_COLOR environment variable
    *   - The FORCE_COLOR environment variable
    *   - any command-line specified color mode (if enabled)
    */
  def global: ColorMode = _curr.get()

private val ansiRegex = "\u001B\\[[;\\d]*m".r

/** Print a message and newline. This should be used instead of `println`
  * because it is aware of the current color mode and can adjust accordingly.
  */
def echo(
    message: Any,
    err: Boolean = false,
    nl: Boolean = true,
    colorMode: ColorMode = ColorMode.Auto
): Unit =
  val useColor = colorMode.orElse(ColorMode.global) match
    case ColorMode.Always => true
    case ColorMode.Never  => false
    case ColorMode.Auto   =>
      if err then clip.util.term.isatty(2)
      else clip.util.term.isatty(1)

  val finalMessage =
    if useColor then message.toString
    else ansiRegex.replaceAllIn(message.toString, "")

  if err then
    if nl then System.err.println(finalMessage)
    else System.err.print(finalMessage)
  else if nl then System.out.println(finalMessage)
  else System.out.print(finalMessage)

val ResetAll = "\u001B[0m"

enum AnsiColor(val n: Int):
  case Black extends AnsiColor(30)
  case Red extends AnsiColor(31)
  case Green extends AnsiColor(32)
  case Yellow extends AnsiColor(33)
  case Blue extends AnsiColor(34)
  case Magenta extends AnsiColor(35)
  case Cyan extends AnsiColor(36)
  case White extends AnsiColor(37)
  case Reset extends AnsiColor(39)
  case BrightBlack extends AnsiColor(90)
  case BrightRed extends AnsiColor(91)
  case BrightGreen extends AnsiColor(92)
  case BrightYellow extends AnsiColor(93)
  case BrightBlue extends AnsiColor(94)
  case BrightMagenta extends AnsiColor(95)
  case BrightCyan extends AnsiColor(96)
  case BrightWhite extends AnsiColor(97)

  def toAnsi(offset: Int = 0): String = s"\u001B[${n + offset}m"

enum Color:
  case Basic(c: AnsiColor)
  case Number(n: Int)
  case Rgb(r: Int, g: Int, b: Int)

  def toAnsi(offset: Int = 0): String = this match
    case Basic(c)     => c.toAnsi(offset)
    case Number(n)    => s"\u001B[${38 + offset};5;${n}m"
    case Rgb(r, g, b) => s"\u001B[${38 + offset};2;${r};${g};${b}m"

object Color:

  // using old-school implicit conversions until `into` is no longer experimental
  import scala.language.implicitConversions
  implicit def intToColor(n: Int): Color = Color.Number(n)
  implicit def tupleToColor(rgb: (Int, Int, Int)): Color =
    Color.Rgb(rgb._1, rgb._2, rgb._3)
  implicit def ansiColorToColor(c: AnsiColor): Color = Color.Basic(c)

// null == no change
case class State(
  fg: Color = null,
  bg: Color = null,
  bold: Boolean | Null = false,
  dim: Boolean | Null = false,
  underline: Boolean | Null = false,
  overline: Boolean | Null = false,
  italic: Boolean | Null = false,
  blink: Boolean | Null = false,
  reverse: Boolean | Null = false,
  strikethrough: Boolean | Null = false
)

trait Tag:
  def applyTo(
    builder: StringBuilder,
    state: State = State()
  ): Unit
  def render(): String =
    val builder = new StringBuilder
    builder ++= ResetAll
    applyTo(builder)
    builder ++= ResetAll
    builder.result()
object Tag:
  import scala.language.implicitConversions
  implicit def stringToTag(s: String): Tag = Raw(s)

class Raw(s: String) extends Tag:
  override def applyTo(builder: StringBuilder, state: State): Unit =
    if state.fg != null then builder ++= state.fg.toAnsi(0)
    if state.bg != null then builder ++= state.bg.toAnsi(10)
    if state.bold != null then
      if state.bold.asInstanceOf[Boolean] then builder ++= "\u001B[1m"
      else builder ++= "\u001B[22m"
    if state.dim != null then
      if state.dim.asInstanceOf[Boolean] then builder ++= "\u001B[2m"
      else builder ++= "\u001B[22m"
    if state.underline != null then
      if state.underline.asInstanceOf[Boolean] then builder ++= "\u001B[4m"
      else builder ++= "\u001B[24m"
    if state.overline != null then
      if state.overline.asInstanceOf[Boolean] then builder ++= "\u001B[53m"
      else builder ++= "\u001B[55m"
    if state.italic != null then
      if state.italic.asInstanceOf[Boolean] then builder ++= "\u001B[3m"
      else builder ++= "\u001B[23m"
    if state.blink != null then
      if state.blink.asInstanceOf[Boolean] then builder ++= "\u001B[5m"
      else builder ++= "\u001B[25m"
    if state.reverse != null then
      if state.reverse.asInstanceOf[Boolean] then builder ++= "\u001B[7m"
      else builder ++= "\u001B[27m"
    if state.strikethrough != null then
      if state.strikethrough.asInstanceOf[Boolean] then builder ++= "\u001B[9m"
      else builder ++= "\u001B[29m"
    builder ++= s
  override def toString(): String = render()

class Styled(
  fg: Color = null,
  bg: Color = null,
  bold: Boolean | Null = null,
  dim: Boolean | Null = null,
  underline: Boolean | Null = null,
  overline: Boolean | Null = null,
  italic: Boolean | Null = null,
  blink: Boolean | Null = null,
  reverse: Boolean | Null = null,
  strikethrough: Boolean | Null = null,
  reset: Boolean = true,
  children: Seq[Tag] = Seq.empty
) extends Tag:
  override def applyTo(builder: StringBuilder, state: State): Unit =
    for child <- children do
      val newState = State(
        if fg != null then fg else state.fg,
        if bg != null then bg else state.bg,
        if bold != null then bold else state.bold,
        if dim != null then dim else state.dim,
        if underline != null then underline else state.underline,
        if overline != null then overline else state.overline,
        if italic != null then italic else state.italic,
        if blink != null then blink else state.blink,
        if reverse != null then reverse else state.reverse,
        if strikethrough != null then strikethrough else state.strikethrough
      )

      child.applyTo(builder, newState)

  override def toString() = render()

/** Style text with ANSI escape codes.
  *
  * By default, the style is reset after the text. This can be disabled by
  * setting `reset` to false.
  */
def style(
    fg: Color = null,
    bg: Color = null,
    bold: Boolean | Null = null,
    dim: Boolean | Null = null,
    underline: Boolean | Null = null,
    overline: Boolean | Null = null,
    italic: Boolean | Null = null,
    blink: Boolean | Null = null,
    reverse: Boolean | Null = null,
    strikethrough: Boolean | Null = null,
    reset: Boolean = true
)(text: Tag*): Tag = Styled(
  fg,
  bg,
  bold,
  dim,
  underline,
  overline,
  italic,
  blink,
  reverse,
  strikethrough,
  reset,
  text
)
  // val buffer = new StringBuilder

  // if fg != null then buffer ++= fg.toAnsi(0)
  // if bg != null then buffer ++= bg.toAnsi(10)
  // if bold != null then
  //   if bold.asInstanceOf[Boolean] then buffer ++= "\u001B[1m"
  //   else buffer ++= "\u001B[22m"
  // if dim != null then
  //   if dim.asInstanceOf[Boolean] then buffer ++= "\u001B[2m"
  //   else buffer ++= "\u001B[22m"
  // if underline != null then
  //   if underline.asInstanceOf[Boolean] then buffer ++= "\u001B[4m"
  //   else buffer ++= "\u001B[24m"
  // if overline != null then
  //   if overline.asInstanceOf[Boolean] then buffer ++= "\u001B[53m"
  //   else buffer ++= "\u001B[55m"
  // if italic != null then
  //   if italic.asInstanceOf[Boolean] then buffer ++= "\u001B[3m"
  //   else buffer ++= "\u001B[23m"
  // if blink != null then
  //   if blink.asInstanceOf[Boolean] then buffer ++= "\u001B[5m"
  //   else buffer ++= "\u001B[25m"
  // if reverse != null then
  //   if reverse.asInstanceOf[Boolean] then buffer ++= "\u001B[7m"
  //   else buffer ++= "\u001B[27m"
  // if strikethrough != null then
  //   if strikethrough.asInstanceOf[Boolean] then buffer ++= "\u001B[9m"
  //   else buffer ++= "\u001B[29m"

  // buffer ++= text
  // if reset then buffer ++= ResetAll
  // buffer.result()

def pause(message: String = "Press Enter to continue..."): Unit =
  if clip.util.term.isatty(System.in) then
    clip.util.termui.echo(message, nl = false)
    scala.io.StdIn.readLine()

/** Launches the given URL in the user's preferred application.
  *
  * @param url
  *   the URL to launch (can also be a file path)
  * @return
  *   the exit code of the launched process, or 127 if launching failed
  *
  * // TODO: support non-linux platforms
  */
def launch(url: String): Int =
  try os.proc("xdg-open", url).call(check = false).exitCode
  catch
    case _: java.io.IOException =>
      System.err.println(
        s"Could not launch '$url'. Please open it manually."
      )
      127

import java.io.OutputStream
import java.io.InputStream

class Bar(
    size: Long,
    label: String = "",
    out: java.io.PrintStream = System.err,
    force: Boolean = false
):

  private val sizeWidth = if size >= 0 then size.toString().length() else 0

  val windowWidth = clip.util.term.sizeOrDefault()._2
  val active = force || clip.util.term.isatty(out)
  val useColor = clip.util.termui.ColorMode.global.useColor(out)

  def update(n: Long): Unit =
    if active then
      if size >= 0 then

        val progressWidth = windowWidth
          - label.length()
          - (sizeWidth * 2 + 5) // for "(<n> / <size>)"
          - 6 // for "[XXX%]"
          - 2 // for brackets
          - 3 // for spaces between groups

        val ratio = n.toDouble / size
        val filled = (ratio * progressWidth).toInt

        out.print("\r") // go to beginning of line
        out.print(label)
        out.print(String.format(s" (%${sizeWidth}d / %${sizeWidth}d)", n, size))
        out.print(f" [${ratio * 100}%3.0f%%]")

        out.print(" [")
        if useColor then out.print(clip.util.termui.AnsiColor.Green.toAnsi())
        for _ <- 0 until filled do out.print("=")
        for _ <- filled until progressWidth do out.print(" ")

        if useColor then out.print(clip.util.termui.AnsiColor.Reset.toAnsi())
        out.print("]")
      else out.print(s"\r$label [$n items processed]")

class progressbar(
    label: String = "",
    out: java.io.PrintStream = System.err,
    force: Boolean = false
):

  private def withBar[A](size: Long)(f: Bar => A): A =
    out.print(label) // always print label, even if no TTY

    if force || clip.util.term.isatty(out) then
      val bar = Bar(size, label, out, force)
      val showCursor = new Thread(() =>
        out.print("\u001b[?25h") // show cursor
        out.println()
      )
      Runtime.getRuntime().addShutdownHook(showCursor)

      val result =
        try
          out.print("\u001b[?25l") // hide cursor
          bar.update(0)
          f(bar)
        finally
          Runtime.getRuntime().removeShutdownHook(showCursor)
          out.print("\u001b[?25h")
          out.println()
      result
    else
      val result = f(Bar(size, label, out, force)) // no-op bar
      out.println()
      result

  def fixed[R](size: Long)(fn: Bar => R) = withBar(size)(fn)

  class CollectionWrapper[A](underlying: Iterable[A]):
    def foreach(f: A => Any): Unit =
      withBar(underlying.size): bar =>
        val it = underlying.iterator
        var n: Long = 0
        while it.hasNext do
          val elem = it.next()
          n += 1
          f(elem)
          bar.update(n)

  def wrap[A](
      items: Iterable[A]
  ): CollectionWrapper[A] = new CollectionWrapper[A](items)

  def wrap(underlying: geny.Readable): geny.Readable = new geny.Readable:
    override def readBytesThrough[T](f: InputStream => T): T =
      underlying.readBytesThrough: stream =>
        withBar(underlying.contentLength.getOrElse(-1L)): bar =>
          val istream = new InputStream:
            var n: Long = 0
            override def read(): Int =
              val r = stream.read()
              if r != -1 then n += 1
              bar.update(n)
              r
            override def read(b: Array[Byte]): Int =
              val r = stream.read(b)
              if r != -1 then n += r
              bar.update(n)
              r
            override def read(b: Array[Byte], off: Int, len: Int): Int =
              val r = stream.read(b, off, len)
              if r != -1 then n += r
              bar.update(n)
              r
          f(istream)

def tabulate(
  headers: Iterable[String],
  lines: Iterable[Iterable[String]],
  sep: String = "   ",
  out: java.io.PrintStream = System.out
): Unit =
  val allLines = headers +: lines.toSeq

  if allLines.isEmpty then return

  val nCols = allLines.map(_.size).max
  val colWidths = Array.fill(nCols)(0)

  for line <- allLines do
    for (col, i) <- line.zipWithIndex do
      colWidths(i) = math.max(colWidths(i), col.length)

  def formatLine(line: Iterable[String]): String =
    line.zipWithIndex.map: (col, i) =>
      String.format(s"%-${colWidths(i)}s", col)
    .mkString(sep)

  out.println(formatLine(headers))
  for line <- lines do
    out.println(formatLine(line))

