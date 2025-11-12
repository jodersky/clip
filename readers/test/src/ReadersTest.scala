import utest.*
import clip.readers.FromString

object ReadersTest extends TestSuite:

  def read[A](input: String)(using reader: FromString[A]) = reader.fromString(input)
  def assertError[A: FromString](input: String, content: String) =
    val result = read[A](input)
    assert(result.isInstanceOf[Left[?, ?]])
    assert(result.asInstanceOf[Left[String, ?]].left.get.contains(content))

  val tests = Tests:

    test("stringReader"):
      test("success"):
        assert(read[String]("hello") == Right("hello"))
        assert(read[String]("") == Right(""))
        assert(read[String]("with spaces") == Right("with spaces"))

    test("booleanReader"):
      test("success"):
        assert(read[Boolean]("true") == Right(true))
        assert(read[Boolean]("false") == Right(false))

    test("integralReader"):
      test("Int"):
        assert(read[Int]("42") == Right(42))
        assert(read[Int]("-100") == Right(-100))
        assert(read[Int]("0") == Right(0))
        assertError[Int]("not a number", "not an integral number")
        assertError[Int]("3.14", "not an integral number")
      test("Long"):
        assert(read[Long]("9223372036854775807") == Right(9223372036854775807L))
        assert(read[Long]("-9223372036854775808") == Right(-9223372036854775808L))
      test("Short"):
        assert(read[Short]("32767") == Right(32767.toShort))
        assert(read[Short]("-32768") == Right(-32768.toShort))
      test("Byte"):
        assert(read[Byte]("127") == Right(127.toByte))
        assert(read[Byte]("-128") == Right(-128.toByte))

    test("floatReader"):
      test("success"):
        assert(read[Float]("3.14") == Right(3.14f))
        assert(read[Float]("-2.5") == Right(-2.5f))
        assert(read[Float]("0.0") == Right(0.0f))
        assert(read[Float]("42") == Right(42.0f))
      test("error"):
        assertError[Float]("not a number", "not a valid float")

    test("doubleReader"):
      test("success"):
        assert(read[Double]("3.14159265359") == Right(3.14159265359))
        assert(read[Double]("-2.5") == Right(-2.5))
        assert(read[Double]("0.0") == Right(0.0))
        assert(read[Double]("42") == Right(42.0))
      test("error"):
        assertError[Double]("not a number", "not a valid double")

    test("pathReader"):
      test("abs"):
        assert(read[os.FilePath]("/tmp/test.txt") == Right(os.root / "tmp" / "test.txt"))
      test("relative"):
        assert(read[os.FilePath]("../test") == Right(os.rel / ".." / "test"))
      test("sub"):
        assert(read[os.FilePath]("foo/bar.txt") == Right(os.sub / "foo" / "bar.txt"))

    test("absPathReader"):
      test("abs"):
        assert(read[os.Path]("/tmp/test") == Right(os.root / "tmp" / "test"))
      test("relative"):
        assert(read[os.Path]("../foo") == Right(os.pwd / ".." / "foo"))
      test("sub"):
        assert(read[os.Path]("foo/bar") == Right(os.pwd / "foo" / "bar"))

    test("relPathReader"):
      test("abs"):
        assertError[os.RelPath]("/tmp/test", "not a valid relative path")
      test("relative"):
        assert(read[os.RelPath]("../foo") == Right(os.rel / ".." / "foo"))
      test("sub"):
        assert(read[os.RelPath]("foo/bar") == Right(os.sub / "foo" / "bar"))

    test("subPathReader"):
      test("abs"):
        assertError[os.SubPath]("/tmp/test", "not a valid sub path")
      test("relative"):
        assertError[os.SubPath]("../foo", "not a valid sub path")
      test("sub"):
        assert(read[os.SubPath]("foo/bar") == Right(os.sub / "foo" / "bar"))

    test("jPathReader"):
      test("success"):
        assert(read[java.nio.file.Path]("/tmp/test.txt") == Right(java.nio.file.Paths.get("/tmp/test.txt")))
        assert(read[java.nio.file.Path]("relative/path") == Right(java.nio.file.Paths.get("relative/path")))

    test("jFileReader"):
      test("success"):
        assert(read[java.io.File]("/tmp/test.txt") == Right(new java.io.File("/tmp/test.txt")))
        assert(read[java.io.File]("relative/path") == Right(new java.io.File("relative/path")))

    test("tupleReader"):
      test("success"):
        assert(read[(String, String)]("key=value") == Right(("key", "value")))
        assert(read[(String, String)]("name=John Doe") == Right(("name", "John Doe")))
        assert(read[(String, String)]("a=b=c") == Right(("a", "b=c")))
      test("int value"):
        assert(read[(String, Int)]("port=8080") == Right(("port", 8080)))
      test("error - no value"):
        assertError[(String, String)]("keyonly", "expected value after key")
      test("error - invalid value type"):
        assertError[(String, Int)]("port=notanumber", "not an integral number")

    test("optionReader"):
      test("success"):
        assert(read[Option[Int]]("42") == Right(Some(42)))
        assert(read[Option[Int]]("-100") == Right(Some(-100)))
      test("error propagation"):
        assertError[Option[Int]]("not a number", "not an integral number")

    test("durationReader"):
      test("success"):
        import scala.concurrent.duration.*
        assert(read[Duration]("5 seconds") == Right(5.seconds))
        assert(read[Duration]("10 minutes") == Right(10.minutes))
        assert(read[Duration]("1 hour") == Right(1.hour))
        assert(read[Duration]("2 days") == Right(2.days))
        assert(read[Duration]("Inf") == Right(Duration.Inf))
      test("error"):
        import scala.concurrent.duration.*
        assertError[Duration]("invalid", "not a valid duration")

    test("finiteDurationReader"):
      test("success"):
        import scala.concurrent.duration.*
        assert(read[FiniteDuration]("5 seconds") == Right(5.seconds))
        assert(read[FiniteDuration]("10 minutes") == Right(10.minutes))
        assert(read[FiniteDuration]("1 hour") == Right(1.hour))
        assert(read[FiniteDuration]("2 days") == Right(2.days))
      test("error - infinite duration"):
        import scala.concurrent.duration.*
        assertError[FiniteDuration]("Inf", "expected a finite duration")
      test("error"):
        import scala.concurrent.duration.*
        assertError[Duration]("invalid", "not a valid duration")

    test("instantReader"):
      test("success"):
        assert(read[java.time.Instant]("2023-01-15T10:30:00Z") == Right(java.time.Instant.parse("2023-01-15T10:30:00Z")))
      test("success with milliseconds"):
        assert(read[java.time.Instant]("2023-01-15T10:30:00.123Z") == Right(java.time.Instant.parse("2023-01-15T10:30:00.123Z")))
      test("error - no timezone"):
        assertError[java.time.Instant]("2023-01-15T10:30:00", "not a valid instant")
      test("error - invalid format"):
        assertError[java.time.Instant]("not a timestamp", "not a valid instant")

    test("zonedDateTimeReader"):
      test("success"):
        read[java.time.ZonedDateTime]("2023-01-15T10:30:00+01:00[Europe/Paris]") match
          case Right(zdt) =>
            assert(zdt.getYear == 2023)
            assert(zdt.getMonthValue == 1)
            assert(zdt.getDayOfMonth == 15)
          case _ => assert(false)
      test("error"):
        assertError[java.time.ZonedDateTime]("invalid", "not a zoned date and time")

    test("LocalDateTimeReader"):
      test("success"):
        read[java.time.LocalDateTime]("2023-01-15T10:30:00") match
          case Right(ldt) =>
            assert(ldt.getYear == 2023)
            assert(ldt.getMonthValue == 1)
            assert(ldt.getDayOfMonth == 15)
            assert(ldt.getHour == 10)
            assert(ldt.getMinute == 30)
          case _ => assert(false)
      test("error"):
        assertError[java.time.LocalDateTime]("invalid", "not a local date and time")

    test("LocalDateReader"):
      test("success"):
        read[java.time.LocalDate]("2023-01-15") match
          case Right(ld) =>
            assert(ld.getYear == 2023)
            assert(ld.getMonthValue == 1)
            assert(ld.getDayOfMonth == 15)
          case _ => assert(false)
      test("error"):
        assertError[java.time.LocalDate]("invalid", "not a local date")

    test("LocalTime"):
      test("success"):
        read[java.time.LocalTime]("10:30:00") match
          case Right(lt) =>
            assert(lt.getHour == 10)
            assert(lt.getMinute == 30)
            assert(lt.getSecond == 0)
          case _ => assert(false)
        read[java.time.LocalTime]("23:59:59.999") match
          case Right(lt) =>
            assert(lt.getHour == 23)
            assert(lt.getMinute == 59)
          case _ => assert(false)
      test("error"):
        assertError[java.time.LocalTime]("invalid", "not a local time")

    test("RangeReader"):
      test("success"):
        assert(read[Range]("1..10") == Right(1 to 10))
        assert(read[Range]("0..5") == Right(0 to 5))
        assert(read[Range]("-5..5") == Right(-5 to 5))
        read[Range]("1..10") match
          case Right(range) =>
            assert(range.start == 1)
            assert(range.end == 10)
            assert(range.size == 10)
          case _ => assert(false)
      test("error - invalid format"):
        assertError[Range]("1-10", "expected 'from..to'")
      test("error - non-numeric"):
        assertError[Range]("a..b", "not a numeric range")
