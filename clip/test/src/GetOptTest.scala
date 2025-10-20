import utest.*
import clip.getopt.ParamDecl
import clip.getopt.ParseResult
import clip.getopt.Arg
import clip.getopt.parse

object GetOptTest extends TestSuite:
  val tests = Tests:

    test("empty"):
      val result = parse(Nil, Nil.iterator)
      assert(result.grouped.isEmpty)
      assert(result.remainder.isEmpty)

    test("positional"):
      val a = ParamDecl("a")
      val b = ParamDecl("b")
      val params = Seq(a, b)

      test("missing"):
        val result = parse(params, Nil.iterator)
        assert(result.grouped(a.name).isEmpty)
        assert(result.grouped(b.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("partial missing"):
        val result = parse(params, List("value").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("value"))))
        assert(result.grouped(b.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("ok"):
        val result = parse(params, List("a", "b").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("b"))))
        assert(result.remainder.isEmpty)

      test("too many"):
        val result = parse(params, List("a", "b", "c").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("b"))))
        assert(result.remainder.isDefined)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "c")
        assert(!remainder.hasNext)

      test("too many with named"):
        val result = parse(params, List("a", "b", "--c").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("b"))))
        assert(result.remainder.isDefined)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--c")
        assert(!remainder.hasNext)

      test("too many with named out of order"):
        val result = parse(params, List("a", "--c", "b").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name).isEmpty)
        assert(result.remainder.isDefined)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--c")
        assert(remainder.next() == "b")
        assert(!remainder.hasNext)

    test("positional repeated"):
      val a = ParamDecl("a")
      val b = ParamDecl("b", repeats = true)
      val params = Seq(a, b)

      test("ok"):
        val result = parse(params, List("a", "b", "c", "d").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("b")), Arg("b", Some("c")), Arg("b", Some("d"))))
        assert(result.remainder.isEmpty)

      test("ok2"):
        val result = parse(params, List("a", "--b", "c", "d").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name).isEmpty)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--b")
        assert(remainder.next() == "c")
        assert(remainder.next() == "d")
        assert(!remainder.hasNext)

      test("ok3"):
        val result = parse(params, List("a", "--", "c", "d").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("c")), Arg("b", Some("d"))))
        assert(result.remainder.isEmpty)

      test("ok4"):
        val result = parse(params, List("a", "--", "c", "--d").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("c")), Arg("b", Some("--d"))))
        assert(result.remainder.isEmpty)

    test("positional end of named"):
      val a = ParamDecl("a", endOfNamed = true)
      val b = ParamDecl("b", repeats = true)
      val result = parse(Seq(a, b), List("a", "--", "c", "--d").iterator)
        assert(result.grouped(a.name) == Seq(Arg("a", Some("a"))))
        assert(result.grouped(b.name) == Seq(Arg("b", Some("--")), Arg("b", Some("c")), Arg("b", Some("--d"))))
        assert(result.remainder.isEmpty)

    test("named args"):
      val p1 = ParamDecl("--p1")
      val p2 = ParamDecl("--p2")
      val params = Seq(p1, p2)

      test("missing"):
        val result = parse(params, Nil.iterator)
        assert(result.grouped(p1.name).isEmpty)
        assert(result.grouped(p2.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("partial"):
        val result = parse(params, List("--p1").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("ok"):
        val result = parse(params, List("--p1", "--p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", None)))
        assert(result.remainder.isEmpty)

      test("too many"):
        val result = parse(params, List("--p1", "--p2", "--p3").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", None)))
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--p3")
        assert(!remainder.hasNext)

      test("too many out of order"):
        val result = parse(params, List("--p1", "--p3", "--p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name).isEmpty)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--p3")
        assert(remainder.next == "--p2")
        assert(!remainder.hasNext)

      test("too many out of order with pos"):
        val result = parse(params, List("--p2", "--p1", "--p3", "p0", "--p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", None)))
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--p3")
        assert(remainder.next == "p0")
        assert(remainder.next == "--p2")
        assert(!remainder.hasNext)

      test("with arg"):
        val result = parse(params, List("--p1", "p0", "--p2=--p3", "extra").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", Some("p0"))))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", Some("--p3"))))
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "extra")
        assert(!remainder.hasNext)

    test("named flag"):
      val p1 = ParamDecl("--p1", flag=true)
      val p2 = ParamDecl("--p2")
      val params = Seq(p1, p2)

      test("missing"):
        val result = parse(params, Nil.iterator)
        assert(result.grouped(p1.name).isEmpty)
        assert(result.grouped(p2.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("partial"):
        val result = parse(params, List("--p1").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("ok"):
        val result = parse(params, List("--p1", "--p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", None)))
        assert(result.remainder.isEmpty)

      test("too many"):
        val result = parse(params, List("--p1", "--p2", "--p3").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", None)))
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--p3")
        assert(!remainder.hasNext)

      test("too many out of order"):
        val result = parse(params, List("--p1", "--p3", "--p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name).isEmpty)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--p3")
        assert(remainder.next == "--p2")
        assert(!remainder.hasNext)

      test("too many out of order with pos"):
        val result = parse(params, List("--p2", "--p1", "--p3", "p0", "--p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", None)))
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "--p3")
        assert(remainder.next == "p0")
        assert(remainder.next == "--p2")
        assert(!remainder.hasNext)

      test("with arg"):
        val result = parse(params, List("--p1", "p0", "--p2=--p3", "extra").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", None)))
        assert(result.grouped(p2.name).isEmpty)
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "p0")
        assert(remainder.next == "--p2=--p3")
        assert(remainder.next == "extra")
        assert(!remainder.hasNext)

      test("with arg override"):
        val result = parse(params, List("--p1=p0", "--p2=--p3", "extra").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("--p1", Some("p0"))))
        assert(result.grouped(p2.name) == Seq(Arg("--p2", Some("--p3"))))
        val (unknown, remainder) = result.remainder.get
        assert(unknown == "extra")
        assert(!remainder.hasNext)

    test("short multichar"):
      val p1 = ParamDecl("-p1")
      val p2 = ParamDecl("-p2", repeats = true)
      val params = Seq(p1, p2)

      test("embedded"):
        val result = parse(params, List("-p1=a", "-p2=b").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("-p1", Some("a"))))
        assert(result.grouped(p2.name) == Seq(Arg("-p2", Some("b"))))
        assert(result.remainder.isEmpty)

      test("non embedded"):
        val result = parse(params, List("-p1", "a", "-p2", "b", "-p2").iterator)
        assert(result.grouped(p1.name) == Seq(Arg("-p1", Some("a"))))
        assert(result.grouped(p2.name) == Seq(Arg("-p2", Some("b")), Arg("-p2", None)))
        assert(result.remainder.isEmpty)

    test("short flags"):
      val a = ParamDecl("-a", flag = true)
      val b = ParamDecl("-b", flag = true)
      val c = ParamDecl("-c", flag = true)
      val params = Seq(a, b, c)

      test("all"):
        val result = parse(params, List("-abc").iterator)
        assert(result.grouped(a.name) == Seq(Arg("-a", None)))
        assert(result.grouped(b.name) == Seq(Arg("-b", None)))
        assert(result.grouped(c.name) == Seq(Arg("-c", None)))
        assert(result.remainder.isEmpty)

      test("order"):
        val result = parse(params, List("-cab").iterator)
        assert(result.grouped(a.name) == Seq(Arg("-a", None)))
        assert(result.grouped(b.name) == Seq(Arg("-b", None)))
        assert(result.grouped(c.name) == Seq(Arg("-c", None)))
        assert(result.remainder.isEmpty)

      test("partial"):
        val result = parse(params, List("-cb").iterator)
        assert(result.grouped(a.name).isEmpty)
        assert(result.grouped(b.name) == Seq(Arg("-b", None)))
        assert(result.grouped(c.name) == Seq(Arg("-c", None)))
        assert(result.remainder.isEmpty)

      test("partial2"):
        val result = parse(params, List("-cb", "-a").iterator)
        assert(result.grouped(a.name) == Seq(Arg("-a", None)))
        assert(result.grouped(b.name) == Seq(Arg("-b", None)))
        assert(result.grouped(c.name) == Seq(Arg("-c", None)))
        assert(result.remainder.isEmpty)

    test("options1"):
      val d = ParamDecl("-D")
      val params = Seq(d)

      test("no value"):
        val result = parse(params, List("-D").iterator)
        assert(result.grouped(d.name) == Seq(Arg("-D", None)))
        assert(result.remainder.isEmpty)

      test("with value"):
        val result = parse(params, List("-Dhello").iterator)
        assert(result.grouped(d.name) == Seq(Arg("-D", Some("hello"))))
        assert(result.remainder.isEmpty)

      test("with value separate"):
        val result = parse(params, List("-D", "hello").iterator)
        assert(result.grouped(d.name) == Seq(Arg("-D", Some("hello"))))
        assert(result.remainder.isEmpty)

      test("with value embedded"):
        val result = parse(params, List("-D=hello").iterator)
        assert(result.grouped(d.name) == Seq(Arg("-D", Some("hello"))))
        assert(result.remainder.isEmpty)

      test("with value embedded equals"):
        val result = parse(params, List("-D=hello=world").iterator)
        assert(result.grouped(d.name) == Seq(Arg("-D", Some("hello=world"))))
        assert(result.remainder.isEmpty)

      test("with separate equals"):
        val result = parse(params, List("-D", "a=b").iterator)
        assert(result.grouped(d.name) == Seq(Arg("-D", Some("a=b"))))
        assert(result.remainder.isEmpty)

    test("short mixed"):
      val a = ParamDecl("-a", flag=true)
      val b = ParamDecl("-b")
      val c = ParamDecl("c")
      val params = Seq(a, b, c)

      test("ok1"):
        val result = parse(params, List("-bhello").iterator)
        assert(result.grouped(a.name).isEmpty)
        assert(result.grouped(b.name) == Seq(Arg("-b", Some("hello"))))
        assert(result.grouped(c.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("ok2"):
        val result = parse(params, List("-bahello").iterator)
        assert(result.grouped(a.name).isEmpty)
        assert(result.grouped(b.name) == Seq(Arg("-b", Some("ahello"))))
        assert(result.grouped(c.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("ok3"):
        val result = parse(params, List("-b", "hello").iterator)
        assert(result.grouped(a.name).isEmpty)
        assert(result.grouped(b.name) == Seq(Arg("-b", Some("hello"))))
        assert(result.grouped(c.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("withflag1"):
        val result = parse(params, List("-abhello").iterator)
        assert(result.grouped(a.name) == Seq(Arg("-a", None)))
        assert(result.grouped(b.name) == Seq(Arg("-b", Some("hello"))))
        assert(result.grouped(c.name).isEmpty)
        assert(result.remainder.isEmpty)

      test("withflag2"):
        val result = parse(params, List("-ab", "hello").iterator)
        assert(result.grouped(a.name) == Seq(Arg("-a", None)))
        assert(result.grouped(b.name) == Seq(Arg("-b", None)))
        assert(result.grouped(c.name) == Seq(Arg("c", Some("hello"))))
        assert(result.remainder.isEmpty)

      test("withflag3"):
        val result = parse(params, List("-ba", "hello").iterator)
        assert(result.grouped(a.name).isEmpty)
        assert(result.grouped(b.name) == Seq(Arg("-b", Some("a"))))
        assert(result.grouped(c.name) == Seq(Arg("c", Some("hello"))))
        assert(result.remainder.isEmpty)
