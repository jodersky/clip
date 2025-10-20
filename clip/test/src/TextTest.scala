import utest.*
import clip.util.text

object TextTest extends TestSuite:

  val tests = Tests:

    test("wrapper"):
      val input = "This is a sample text that should be wrapped properly according to the specified width."
      val wrapper = text.WrappedLines(input.iterator, 20)
      val result = wrapper.mkString("\n")
      val expected =
        """|This is a sample_
           |text that should be_
           |wrapped properly_
           |according to the_
           |specified width.""".stripMargin
      assert(result == expected.replaceAll("_", " "))

    test("wrapper small"):
      val input = "This is a sample text that should be wrapped properly according to the specified width."
      val wrapper = text.WrappedLines(input.iterator, 6)
      val result = wrapper.mkString("\n")
      val expected =
        """|This_
           |is a_
           |sample
           | text_
           |that s
           |hould_
           |be wra
           |pped_
           |proper
           |ly acc
           |ording
           | to_
           |the sp
           |ecifie
           |d_
           |width.""".stripMargin
      assert(result == expected.replaceAll("_", " "))

    test("cols"):
      val col1 = "This is column one. It has some text that should be wrapped properly. Blablablabla".toSeq
      val col2 = "This is column two. It also has text that should be wrapped properly.".toSeq
      val out = new java.io.ByteArrayOutputStream()
      text.twoCols(col1, col2, 30, 30, "", new java.io.PrintStream(out))
      val result = out.toString()
      val expected =
        """|This is column one. It has    This is column two. It also_
           |some text that should be      has text that should be_
           |wrapped properly. Blablablablawrapped properly.
           |""".stripMargin
      assert(result == expected.replaceAll("_", " "))

    test("cols2"):
      val col1 = "aaaaaaaaaaaaaaaaaaaa"
      val col2 = "bbbbbbbbbbbbbbbbbbbbcccccccccc"
      val out = new java.io.ByteArrayOutputStream()
      text.twoCols(col1, col2, 20, 30, "", new java.io.PrintStream(out))
      val result = out.toString()
      assert(result == col1 + col2 + "\n")

    test("cols3"):
      val col1 = "aaaaaaaaaaaaaaaaaaaaa"
      val col2 = "bbbbbbbbbbbbbbbbbbbbcccccccccc"
      val out = new java.io.ByteArrayOutputStream()
      text.twoCols(col1, col2, 20, 30, "", new java.io.PrintStream(out))
      val result = out.toString()
      assert(result == "aaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbcccccccccc\na\n")

    test("separator"):
      val col1 = "This is column one. It has some text that should be wrapped properly. Blablablabla".toSeq
      val col2 = "This is column two. It also has text that should be wrapped properly.".toSeq
      val out = new java.io.ByteArrayOutputStream()
      text.twoCols(col1, col2, 30, 30, " | ", new java.io.PrintStream(out))
      val result = out.toString()
      val expected =
        """|This is column one. It has     | This is column two. It also_
           |some text that should be       | has text that should be_
           |wrapped properly. Blablablabla | wrapped properly.
           |""".stripMargin
      assert(result == expected.replaceAll("_", " "))
