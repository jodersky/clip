package clip.derivation

trait ReaderApi:

  trait Reader[A]:

    /** Read a value of type A from its string representation. */
    def read(str: String): ReadResult[A]

    /** The name of the type A, used for error messages and help text. */
    def typeName: String
