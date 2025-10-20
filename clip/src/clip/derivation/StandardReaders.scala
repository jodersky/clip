package clip.derivation

trait StandardReaders extends ReaderApi:

  given stringReader: Reader[String] with
    def read(s: String) = ReadResult.Success(s)
    def typeName: String = "string"

  given booleanReader: Reader[Boolean] with
    def read(s: String) = s match
      case "true"  => ReadResult.Success(true)
      case "false" => ReadResult.Success(false)
      case _       => ReadResult.Error(s"'$s' is not a valid boolean")
    def typeName: String = "boolean"

  given integralReader[N](using n: Integral[N]): Reader[N] with
    def read(s: String) = n.parseString(s) match
      case Some(value) => ReadResult.Success(value)
      case None        => ReadResult.Error(s"'$s' is not an integral number")
    def typeName: String = "integer"

  given floatReader: Reader[Float] with
    def read(s: String) =
      try ReadResult.Success(s.toFloat)
      catch
        case _: NumberFormatException =>
          ReadResult.Error(s"'$s' is not a valid float")
    def typeName: String = "float"

  given doubleReader: Reader[Double] with
    def read(s: String) =
      try ReadResult.Success(s.toDouble)
      catch
        case _: NumberFormatException =>
          ReadResult.Error(s"'$s' is not a valid double")
    def typeName: String = "float"

  given pathReader: Reader[os.FilePath] with
    def read(a: String) =
      try ReadResult.Success(os.FilePath(a))
      catch
        case _: IllegalArgumentException =>
          ReadResult.Error(s"'$a' is not a valid path")
    def typeName = "path"

  given absPathReader: Reader[os.Path] with
    def read(a: String) =
      try ReadResult.Success(os.Path(a, os.pwd))
      catch
        case _: IllegalArgumentException =>
          ReadResult.Error(s"'$a' is not a valid path")
    def typeName = "path"

  given relPathReader: Reader[os.RelPath] with
    def read(a: String) =
      try ReadResult.Success(os.RelPath(a))
      catch
        case _: IllegalArgumentException =>
          ReadResult.Error(s"'$a' is not a valid relative path")
    def typeName = "relative path"

  given subPathReader: Reader[os.SubPath] with
    def read(a: String) =
      try ReadResult.Success(os.SubPath(a))
      catch
        case _: IllegalArgumentException =>
          ReadResult.Error(s"'$a' is not a valid sub path")
    def typeName = "sub path"

  given jPathReader: Reader[java.nio.file.Path] with
    def read(a: String) =
      try ReadResult.Success(java.nio.file.Paths.get(a))
      catch
        case _: java.nio.file.InvalidPathException =>
          ReadResult.Error(s"'$a' is not a valid path")
    def typeName: String = "path"

  given jFileReader: Reader[java.io.File] with
    def read(a: String) =
      try ReadResult.Success(java.io.File(a))
      catch case _: Exception => ReadResult.Error(s"'$a' is not a valid path")
    def typeName: String = "path"

  // private class ColonSeparatedReader[E, Col[_] <: Iterable[_]](tn: String)(using er: Reader[E], factory: collection.Factory[E, Col[E]]) extends Reader[Col[E]]:
  //   def read(str: String) =
  //     val parts = str.split(":")
  //     val builder = factory.newBuilder
  //     var i = 0
  //     while i < parts.length do
  //       er.read(parts(i)) match
  //         case ReadResult.Success(elem) => builder += elem
  //         case ReadResult.Error(e) => return ReadResult.Error(e)
  //       i += 1
  //     ReadResult.Success(builder.result())
  //   def typeName: String = tn

  // given pathCollectionReader[Col[_] <: Iterable[os.FilePath]]
  //   (using collection.Factory[os.FilePath, Col[os.FilePath]], Reader[os.FilePath]): Reader[Col[os.FilePath]] =
  //     ColonSeparatedReader("path list")

  // given absPathCollectionReader[Col[_] <: Iterable[os.Path]]
  //   (using collection.Factory[os.Path, Col[os.Path]], Reader[os.Path]): Reader[Col[os.Path]] =
  //     ColonSeparatedReader("absolute path list")

  // given relPathCollectionReader[Col[_] <: Iterable[os.RelPath]]
  //   (using collection.Factory[os.RelPath, Col[os.RelPath]], Reader[os.RelPath]): Reader[Col[os.RelPath]] =
  //     ColonSeparatedReader("relative path list")

  // given subPathCollectionReader[Col[_] <: Iterable[os.SubPath]]
  //   (using collection.Factory[os.SubPath, Col[os.SubPath]], Reader[os.SubPath]): Reader[Col[os.SubPath]] =
  //     ColonSeparatedReader("sub path list")

  // given jPathCollectionReader[Col[_] <: Iterable[java.nio.file.Path]]
  //   (using collection.Factory[java.nio.file.Path, Col[java.nio.file.Path]], Reader[java.nio.file.Path]): Reader[Col[java.nio.file.Path]] =
  //     ColonSeparatedReader("path list")

  // given jFileCollectionReader[Col[_] <: Iterable[java.io.File]]
  //   (using collection.Factory[java.io.File, Col[java.io.File]], Reader[java.io.File]): Reader[Col[java.io.File]] =
  //     ColonSeparatedReader("path list")

  given tupleReader[K, V](using kr: Reader[K], vr: Reader[V]): Reader[(K, V)]
  with
    def read(a: String): ReadResult[(K, V)] =
      a.split("=", 2) match
        case Array(k, v) =>
          val k1 = kr.read(k)
          val v1 = vr.read(v)
          (k1, v1) match
            case (ReadResult.Success(k2), ReadResult.Success(v2)) =>
              ReadResult.Success((k2, v2))
            case (ReadResult.Error(msg), _) => ReadResult.Error(msg)
            case (ReadResult.Success(_), ReadResult.Error(msg)) =>
              ReadResult.Error(msg)
        case Array(k) => ReadResult.Error(s"expected value after key '$k'")
        case _        => ReadResult.Error(s"expected key=value pair")
    def typeName = s"${kr.typeName}=${vr.typeName}"

  given optionReader[A](using elementReader: Reader[A]): Reader[Option[A]] with
    def read(a: String) =
      elementReader.read(a) match
        case ReadResult.Error(message) => ReadResult.Error(message)
        case ReadResult.Success(value) => ReadResult.Success(Some(value))
    def typeName = elementReader.typeName

  given durationReader: Reader[scala.concurrent.duration.Duration] with
    def read(a: String) = try
      ReadResult.Success(scala.concurrent.duration.Duration.create(a))
    catch
      case _: NumberFormatException =>
        ReadResult.Error(s"'$a' is not a valid duration")
    def typeName = "duration"

  given finiteDurationReader: Reader[scala.concurrent.duration.FiniteDuration]
  with
    def read(a: String) = durationReader.read(a) match
      case ReadResult.Success(f: scala.concurrent.duration.FiniteDuration) =>
        ReadResult.Success(f)
      case ReadResult.Success(f: scala.concurrent.duration.Duration) =>
        ReadResult.Error(s"expected a finite duration, but '$a' is infinite")
      case ReadResult.Error(msg) => ReadResult.Error(msg)
    def typeName = "finite duration"

  given instantReader: Reader[java.time.Instant] with
    def read(a: String) = try ReadResult.Success(java.time.Instant.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        ReadResult.Error(
          s"'$a' is not a valid instant in time. The format must follow 'YYYY-MM-DDThh:mm:ss[.S]Z'. Note that the 'T' is literal and the time zone Z must be given."
        )

    def typeName = "timestamp"

  given zonedDateTimeReader: Reader[java.time.ZonedDateTime] with
    def read(a: String) = try
      ReadResult.Success(java.time.ZonedDateTime.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        ReadResult.Error(s"'$a' is not a zoned date and time")
    def typeName = "timestamp"
  given LocalDateTimeReader: Reader[java.time.LocalDateTime] with
    def read(a: String) = try
      ReadResult.Success(java.time.LocalDateTime.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        ReadResult.Error(s"'$a' is not a local date and time")
    def typeName = "local timestamp"

  given LocalDateReader: Reader[java.time.LocalDate] with
    def read(a: String) = try ReadResult.Success(java.time.LocalDate.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        ReadResult.Error(s"'$a' is not a local date")

    def typeName = "local date"

  given LocalTime: Reader[java.time.LocalTime] with
    def read(a: String) = try ReadResult.Success(java.time.LocalTime.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        ReadResult.Error(s"'$a' is not a local time")

    def typeName = "local time"

  given RangeReader: Reader[Range] with
    def read(str: String) = str.split("\\.\\.") match
      case Array(from, to) =>
        try ReadResult.Success(from.toInt to to.toInt)
        catch
          case _: Exception =>
            ReadResult.Error(s"'$str' is not a numeric range")
      case _ => ReadResult.Error("expected 'from..to'")

    def typeName = "from..to"
