package clip.util

case class AbortException(code: Int, message: String) extends Exception(message)

/** Exit the program with an error code.
  *
  * This throws a special exception that is caught by the clip main invoker to
  * exit the program with the given code.
  */
def abort(code: Int = 1, message: String = ""): Nothing =
  throw AbortException(code, message)

trait Output:
  def stream(s: java.io.OutputStream => Unit): Unit

object Output:

  class File(path: os.Path) extends Output:
    def stream(s: java.io.OutputStream => Unit): Unit =
      val stream = os.write.outputStream(path, createFolders = true)
      try
        s(stream)
      finally
        stream.close()

  class Stream(out: java.io.OutputStream) extends Output:
    def stream(s: java.io.OutputStream => Unit): Unit = s(out)

  object Stdout extends Stream(System.out)

trait Input extends geny.Readable:
  def stream[A](f: java.io.InputStream => A): A
  def readBytesThrough[A](f: java.io.InputStream => A): A = stream[A](f)

object Input:

  class File(path: os.Path) extends Input:
    def stream[A](f: java.io.InputStream => A): A =
      val stream = os.read.inputStream(path)
      try
        f(stream)
      finally
        stream.close()

  class Stream(in: java.io.InputStream) extends Input:
    def stream[A](f: java.io.InputStream => A): A = f(in)

  object Stdin extends Stream(System.in)
