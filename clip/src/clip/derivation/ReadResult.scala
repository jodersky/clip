package clip.derivation

enum ReadResult[+A]:
  case Success(value: A) extends ReadResult[A]
  case Error(message: String) extends ReadResult[Nothing]

  def map[B](f: A => B): ReadResult[B] = this match
    case Success(value) => Success(f(value))
    case e: Error       => e

  def flatMap[B](f: A => ReadResult[B]): ReadResult[B] = this match
    case Success(value) => f(value)
    case e: Error       => e
