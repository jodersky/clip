package clip.readers

trait FromString[A]:
  def fromString(str: String): Either[String, A]
  def typeName: String

object FromString:

  given stringFromString: FromString[String] with
    def fromString(s: String) = Right(s)
    def typeName: String = "string"

  given booleanFromString: FromString[Boolean] with
    def fromString(s: String) = s match
      case "true"  => Right(true)
      case "false" => Right(false)
      case _       => Left(s"'$s' is not a valid boolean")
    def typeName: String = "boolean"

  given integralFromString[N](using n: Integral[N]): FromString[N] with
    def fromString(s: String) = n.parseString(s) match
      case Some(value) => Right(value)
      case None        => Left(s"'$s' is not an integral number")
    def typeName: String = "integer"

  given floatFromString: FromString[Float] with
    def fromString(s: String) =
      try Right(s.toFloat)
      catch
        case _: NumberFormatException =>
          Left(s"'$s' is not a valid float")
    def typeName: String = "float"

  given doubleFromString: FromString[Double] with
    def fromString(s: String) =
      try Right(s.toDouble)
      catch
        case _: NumberFormatException =>
          Left(s"'$s' is not a valid double")
    def typeName: String = "float"

  given pathFromString: FromString[os.FilePath] with
    def fromString(a: String) =
      try Right(os.FilePath(a))
      catch
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a valid path")
    def typeName = "path"

  given absPathFromString: FromString[os.Path] with
    def fromString(a: String) =
      try Right(os.Path(a, os.pwd))
      catch
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a valid path")
    def typeName = "path"

  given relPathFromString: FromString[os.RelPath] with
    def fromString(a: String) =
      try Right(os.RelPath(a))
      catch
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a valid relative path")
    def typeName = "relative path"

  given subPathFromString: FromString[os.SubPath] with
    def fromString(a: String) =
      try Right(os.SubPath(a))
      catch
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a valid sub path")
    def typeName = "sub path"

  given jPathFromString: FromString[java.nio.file.Path] with
    def fromString(a: String) =
      try Right(java.nio.file.Paths.get(a))
      catch
        case _: java.nio.file.InvalidPathException =>
          Left(s"'$a' is not a valid path")
    def typeName: String = "path"

  given jFileFromString: FromString[java.io.File] with
    def fromString(a: String) =
      try Right(java.io.File(a))
      catch case _: Exception => Left(s"'$a' is not a valid path")
    def typeName: String = "path"

  // private class ColonSeparatedFromString[E, Col[_] <: Iterable[_]](tn: String)(using er: FromString[E], factory: collection.Factory[E, Col[E]]) extends FromString[Col[E]]:
  //   def fromString(str: String) =
  //     val parts = str.split(":")
  //     val builder = factory.newBuilder
  //     var i = 0
  //     while i < parts.length do
  //       er.read(parts(i)) match
  //         case Right(elem) => builder += elem
  //         case Left(e) => return Left(e)
  //       i += 1
  //     Right(builder.result())
  //   def typeName: String = tn

  // given pathCollectionFromString[Col[_] <: Iterable[os.FilePath]]
  //   (using collection.Factory[os.FilePath, Col[os.FilePath]], FromString[os.FilePath]): FromString[Col[os.FilePath]] =
  //     ColonSeparatedFromString("path list")

  // given absPathCollectionFromString[Col[_] <: Iterable[os.Path]]
  //   (using collection.Factory[os.Path, Col[os.Path]], FromString[os.Path]): FromString[Col[os.Path]] =
  //     ColonSeparatedFromString("absolute path list")

  // given relPathCollectionFromString[Col[_] <: Iterable[os.RelPath]]
  //   (using collection.Factory[os.RelPath, Col[os.RelPath]], FromString[os.RelPath]): FromString[Col[os.RelPath]] =
  //     ColonSeparatedFromString("relative path list")

  // given subPathCollectionFromString[Col[_] <: Iterable[os.SubPath]]
  //   (using collection.Factory[os.SubPath, Col[os.SubPath]], FromString[os.SubPath]): FromString[Col[os.SubPath]] =
  //     ColonSeparatedFromString("sub path list")

  // given jPathCollectionFromString[Col[_] <: Iterable[java.nio.file.Path]]
  //   (using collection.Factory[java.nio.file.Path, Col[java.nio.file.Path]], FromString[java.nio.file.Path]): FromString[Col[java.nio.file.Path]] =
  //     ColonSeparatedFromString("path list")

  // given jFileCollectionFromString[Col[_] <: Iterable[java.io.File]]
  //   (using collection.Factory[java.io.File, Col[java.io.File]], FromString[java.io.File]): FromString[Col[java.io.File]] =
  //     ColonSeparatedFromString("path list")

  given tupleFromString[K, V](using
      kr: FromString[K],
      vr: FromString[V]
  ): FromString[(K, V)] with
    def fromString(a: String): Either[String, (K, V)] =
      a.split("=", 2) match
        case Array(k, v) =>
          val k1 = kr.fromString(k)
          val v1 = vr.fromString(v)
          (k1, v1) match
            case (Right(k2), Right(v2)) =>
              Right((k2, v2))
            case (Left(msg), _)        => Left(msg)
            case (Right(_), Left(msg)) =>
              Left(msg)
        case Array(k) => Left(s"expected value after key '$k'")
        case _        => Left(s"expected key=value pair")
    def typeName = s"${kr.typeName}=${vr.typeName}"

  given optionFromString[A](using elementFromString: FromString[A]): FromString[
    Option[A]
  ] with
    def fromString(a: String) =
      elementFromString.fromString(a) match
        case Left(message) => Left(message)
        case Right(value)  => Right(Some(value))
    def typeName = elementFromString.typeName

  given durationFromString: FromString[scala.concurrent.duration.Duration] with
    def fromString(a: String) = try
      Right(scala.concurrent.duration.Duration.create(a))
    catch
      case _: NumberFormatException =>
        Left(s"'$a' is not a valid duration")
    def typeName = "duration"

  given finiteDurationFromString
      : FromString[scala.concurrent.duration.FiniteDuration] with
    def fromString(a: String) =
      summon[FromString[scala.concurrent.duration.Duration]].fromString(a) match
        case Right(f: scala.concurrent.duration.FiniteDuration) =>
          Right(f)
        case Right(f: scala.concurrent.duration.Duration) =>
          Left(s"expected a finite duration, but '$a' is infinite")
        case Left(msg) => Left(msg)
    def typeName = "finite duration"

  given instantFromString: FromString[java.time.Instant] with
    def fromString(a: String) = try Right(java.time.Instant.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        Left(
          s"'$a' is not a valid instant in time. The format must follow 'YYYY-MM-DDThh:mm:ss[.S]Z'. Note that the 'T' is literal and the time zone Z must be given."
        )

    def typeName = "timestamp"

  given zonedDateTimeFromString: FromString[java.time.ZonedDateTime] with
    def fromString(a: String) = try Right(java.time.ZonedDateTime.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        Left(s"'$a' is not a zoned date and time")
    def typeName = "timestamp"
  given LocalDateTimeFromString: FromString[java.time.LocalDateTime] with
    def fromString(a: String) = try Right(java.time.LocalDateTime.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        Left(s"'$a' is not a local date and time")
    def typeName = "local timestamp"

  given LocalDateFromString: FromString[java.time.LocalDate] with
    def fromString(a: String) = try Right(java.time.LocalDate.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        Left(s"'$a' is not a local date")

    def typeName = "local date"

  given LocalTime: FromString[java.time.LocalTime] with
    def fromString(a: String) = try Right(java.time.LocalTime.parse(a))
    catch
      case ex: java.time.format.DateTimeParseException =>
        Left(s"'$a' is not a local time")

    def typeName = "local time"

  given RangeFromString: FromString[Range] with
    def fromString(str: String) = str.split("\\.\\.") match
      case Array(from, to) =>
        try Right(from.toInt to to.toInt)
        catch
          case _: Exception =>
            Left(s"'$str' is not a numeric range")
      case _ => Left("expected 'from..to'")

    def typeName = "from..to"
