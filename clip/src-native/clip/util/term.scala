package clip.util.term

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@extern
private object libc:
  def isatty(fd: CInt): CInt = extern
  def ioctl(fd: CInt, op: CUnsignedLong, argp: CVoidPtr): CInt = extern

/** Check if the given file descriptor is a terminal
  * @param fd
  *   File descriptor (0 = stdin, 1 = stdout, 2 = stderr)
  * @return
  *   true if the file descriptor is a terminal
  */
def isatty(fd: Int): Boolean =
  libc.isatty(fd) != 0

def isatty(stream: java.io.InputStream | java.io.PrintStream): Boolean =
  val fd = stream match
    case s if s == System.in  => 0
    case s if s == System.out => 1
    case s if s == System.err => 2
    case _                    => return false
  isatty(fd)

/** Get the current size of the terminal in (rows, cols).
  *
  * Note that this is a best-effort function and may not be supported on all
  * platforms. The reason is that it uses the `ioctl` system call with a
  * parameter that is platform-dependent.
  *
  * @return
  *   Some((rows, cols)) if any of stdin, stdout, or stderr are attached to a
  *   terminal (and the current platform is supported), None otherwise
  */
def size(): Option[(Int, Int)] =
  val lti = scala.scalanative.meta.LinktimeInfo
  val tiocgwinsz =
    if lti.isLinux then 0x5413
    else if lti.isMac then 0x40087468
    else return None

  Zone:
    val buffer = stackalloc[UShort](4)
    var r = libc.ioctl(0, tiocgwinsz.toUInt, buffer) // stdin
    if r != 0 then r = libc.ioctl(1, tiocgwinsz.toUInt, buffer) // stdout
    if r != 0 then r = libc.ioctl(2, tiocgwinsz.toUInt, buffer) // stderr
    if r != 0 then return None
    Some((buffer(0).toInt, buffer(1).toInt))

/** Get the current size of the terminal in (rows, cols), or a default of (24,
  * 80)
  */
def sizeOrDefault(): (Int, Int) =
  size().getOrElse((24, 80))

// built-in termios lib is broken on linux https://github.com/scala-native/scala-native/pull/4155
@extern
object linuxtermios:
  import scalanative.unsafe._
  import scalanative.unsafe.Nat._
  import scalanative.posix.sys.types.pid_t

  // these are different than in scalanative.posix.termios
  type tcflag_t = CUnsignedInt
  type cc_t = CUnsignedChar
  type speed_t = CUnsignedInt
  // end difference

  type NCCS = Digit2[_2, _0]
  type c_cc = CArray[cc_t, NCCS]

  type termios = CStruct7[
    tcflag_t, /* c_iflag - input flags   */
    tcflag_t, /* c_oflag - output flags  */
    tcflag_t, /* c_cflag - control flags */
    tcflag_t, /* c_lflag - local flags   */
    c_cc, /* cc_t c_cc[NCCS] - control chars */
    speed_t, /* c_ispeed - input speed   */
    speed_t /* c_ospeed - output speed  */
  ]

  // functions

  def cfgetispeed(termios_p: Ptr[termios]): speed_t = extern
  def cfgetospeed(termios_p: Ptr[termios]): speed_t = extern
  def cfsetispeed(termios_p: Ptr[termios], speed: speed_t): CInt = extern
  def cfsetospeed(termios_p: Ptr[termios], speed: speed_t): CInt = extern
  def tcdrain(fd: CInt): CInt = extern
  def tcflow(fd: CInt, action: CInt): CInt = extern
  def tcflush(fd: CInt, queueSelector: CInt): CInt = extern
  def tcgetattr(fd: CInt, termios_p: Ptr[termios]): CInt = extern
  def tcgetsid(i: CInt): pid_t = extern
  def tcsendbreak(fd: CInt, duration: CInt): CInt = extern
  def tcsetattr(
      fd: CInt,
      optionalActions: CInt,
      termios_p: Ptr[termios]
  ): CInt = extern

def readHidden(): String = Zone:
  import scalanative.posix.termios

  val lti = scala.scalanative.meta.LinktimeInfo

  if lti.isLinux then
    val prevt = alloc[linuxtermios.termios]()
    val newt = alloc[linuxtermios.termios]()
    if linuxtermios
        .tcgetattr(0, prevt) != 0 || linuxtermios.tcgetattr(0, newt) != 0
    then sys.error("tcgetattr failed")
    newt._4 = newt._4 & ~termios.ECHO.toUInt
    if linuxtermios.tcsetattr(0, termios.TCSANOW, newt) != 0 then
      sys.error("tcsetattr failed")
    try
      scala.io.StdIn.readLine()
    finally
      linuxtermios.tcsetattr(0, termios.TCSANOW, prevt)
  else
    val prevt = alloc[termios.termios]()
    val newt = alloc[termios.termios]()
    if termios.tcgetattr(0, prevt) != 0 || termios.tcgetattr(0, newt) != 0 then
      sys.error("tcgetattr failed")
    newt._4 = newt._4 & ~termios.ECHO
    if termios.tcsetattr(0, termios.TCSANOW, newt) != 0 then
      sys.error("tcsetattr failed")
    try
      scala.io.StdIn.readLine()
    finally
      termios.tcsetattr(0, termios.TCSANOW, prevt)
