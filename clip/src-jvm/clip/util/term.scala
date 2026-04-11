package clip.util.term

import java.lang.foreign

/** Check if the given file descriptor is a terminal
  * @param fd
  *   File descriptor (0 = stdin, 1 = stdout, 2 = stderr)
  * @return
  *   true if the file descriptor is a terminal
  */
def isatty(fd: Int): Boolean =
  val linker = foreign.Linker.nativeLinker()
  val isattyAddr = linker.defaultLookup().find("isatty").get()
  val isattySig = foreign.FunctionDescriptor.of(
    foreign.ValueLayout.JAVA_INT, // return value
    foreign.ValueLayout.JAVA_INT // file descriptor
  )
  val isatty = linker.downcallHandle(isattyAddr, isattySig)
  val r = isatty.invoke(fd)
  r != 0

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
  val tiocgwinsz = sys.props("os.name").toLowerCase() match
    case os if os.startsWith("linux")                          => 0x5413
    case os if os.startsWith("mac") || os.startsWith("darwin") => 0x40087468
    case _                                                     => return None

  val linker = foreign.Linker.nativeLinker()
  val ioctlAddr = linker.defaultLookup().find("ioctl").get()
  val ioctlSig = foreign.FunctionDescriptor.of(
    foreign.ValueLayout.JAVA_INT, // return value
    foreign.ValueLayout.JAVA_INT, // descriptor
    foreign.ValueLayout.JAVA_LONG, // op
    foreign.ValueLayout.ADDRESS // pointer to winsize struct
  )
  val ioctl = linker.downcallHandle(
    ioctlAddr,
    ioctlSig,
    foreign.Linker.Option.firstVariadicArg(2)
  )

  val winsizeStruct = foreign.MemoryLayout.structLayout(
    foreign.ValueLayout.JAVA_SHORT.withName("rows"),
    foreign.ValueLayout.JAVA_SHORT.withName("cols"),
    foreign.ValueLayout.JAVA_SHORT.withName("xpix"),
    foreign.ValueLayout.JAVA_SHORT.withName("ypix")
  )

  val arena = foreign.Arena.ofConfined()
  try
    val winsize = arena.allocate(winsizeStruct)
    val TIOCGWINSZ = 0x5413
    var r = ioctl.invoke(0, TIOCGWINSZ.toLong, winsize) // stdin
    if r != 0 then r = ioctl.invoke(1, TIOCGWINSZ.toLong, winsize) // stdout
    if r != 0 then r = ioctl.invoke(2, TIOCGWINSZ.toLong, winsize) // stderr
    if r == 0 then
      val rows = winsize.get(foreign.ValueLayout.JAVA_SHORT, 0).toInt
      val cols = winsize.get(foreign.ValueLayout.JAVA_SHORT, 2).toInt
      Some((rows, cols))
    else None
  finally arena.close()

/** Get the current size of the terminal in (rows, cols), or a default of (24,
  * 80)
  */
def sizeOrDefault(): (Int, Int) =
  size().getOrElse((24, 80))

def readHidden(): String =
  new String(System.console().readPassword())
