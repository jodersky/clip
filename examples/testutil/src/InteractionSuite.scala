import utest._, framework._

class OutputsDifferException(diff: String) extends Exception(diff)

trait InteractionSuite extends TestSuite:

  private val checks = IndexedSeq.newBuilder[(String, () => Unit)]

  protected def test(name: String)(fct: => Unit): Unit = {
    checks += (name -> (() => fct))
  }

  final override def tests: Tests = {
    val ts = checks.result()
    if (ts.isEmpty) {
      Tests.apply(())
    } else {
      val names = Tree("", ts.map((name, _) => Tree(name))*)
      val thunks = new TestCallTree(Right(ts.map((_, method) => new TestCallTree(Left(method())))))
      Tests.apply(names, thunks)
    }
  }

  def exec(commands: Seq[String]): String = {
    val builder = new ProcessBuilder(commands*)
      //.redirectError(ProcessBuilder.Redirect.INHERIT)
      .redirectErrorStream(true)

    // builder.environment.clear()
    //for ((k, v) <- env) builder.environment.put(k, v)
    val process = builder.start()

    val reader = process.getInputStream()
    val out = new java.io.ByteArrayOutputStream
    try {
      val buffer = new Array[Byte](8192)
      var l = 0
      while
        l = reader.read(buffer)
        l != -1
      do
        out.write(buffer, 0, l)

      process.waitFor()
      out.toString()
    } finally {
      process.destroy()
      reader.close()
      process.waitFor()
    }
  }

  def savingStty[A](action: => A) =
    val sttySettings = exec(Seq("stty", "-a")).linesIterator.next()
    try
      action
    finally
          exec(Seq("stty", sttySettings))

  private def assertNoDiff(expected: String, actual: String): Unit =
    if (expected != actual) {
      val diff = exec(
        Seq("diff", "--context", os.temp(expected, deleteOnExit = false).toString, os.temp(actual, deleteOnExit = false).toString)
      )
      val ex = OutputsDifferException(diff)
      ex.setStackTrace(Array())
      throw ex
    }

  val snippetFile = os.Path(sys.env("SNIPPET_FILE"), os.pwd)
  val snippetText = os.read(snippetFile)
  val snippets: Array[String] = snippetText.split("""\n?\$\s+""").tail

  if sys.env.contains("OVERWRITE") then
    val updatedSnippets = for snippet <- snippets yield
      val lines = snippet.linesIterator
      val command = lines.next()

      val actual = exec(Seq("/bin/sh", "-c", command)).linesIterator.toList.mkString("\n")
      s"$$ $command\n$actual\n"
    os.write.over(snippetFile, updatedSnippets.mkString("\n"))
  else
    for snippet <- snippets do
      val lines = snippet.linesIterator
      val command = lines.next()
      val expected = lines.toList.map(_.trim).mkString("\n")

      test(command):
        val actual = exec(Seq("/bin/sh", "-c", command)).linesIterator.toList.map(_.trim).mkString("\n")
        assertNoDiff(expected, actual)
