package clip.util

case class AbortException(code: Int, message: String) extends Exception(message)

/** Exit the program with an error code.
  *
  * This throws a special exception that is caught by the clip main invoker to
  * exit the program with the given code.
  */
def abort(code: Int = 1, message: String = ""): Nothing =
  throw AbortException(code, message)
