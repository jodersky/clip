import utest.*
import os.readLink

object CompTest extends TestSuite:

  def testCompletions(executable: os.Path)(
    tests0: (String, Seq[String])*,
  ) =
    // "clip --\t\t\u0015",

    val tests = tests0.map: (cmd, completions) =>
      cmd.replaceAll("app", executable.toString) -> completions

    val tmp = os.temp.dir(prefix="completion-test-")
    val script = tmp / "completion.sh"
    os.proc(executable, s"--completion", s"bash,${executable}")
      .call(stdout = script, cwd = os.pwd)

    // We can't pass --noedit to bash to disable output, since
    // it also suppresses bash completions. Hence, we echo a marker
    // line after setup, which we can use to drop uninteresting output.
    val in = Seq(
      "source /usr/share/bash-completion/bash_completion",
      s"source $script",
    ) ++ tests.map((cmd, _) =>
      s"$cmd\t"
    ) ++ Seq("exit 0")

    val log = tmp / "completion.log"

    val res = os.proc(
      "bash", "--norc", "--noprofile", "-i")
      .call(
        env=Map(
          "PS1" -> "",
          "PROMPT_COMMAND" -> "",
          "BASH_COMP_DEBUG_FILE" -> log.toString(),
        ),
        stdin = in.mkString("\r", "\r", "\r"),
        stdout = os.Pipe,
        timeout = 20000,
        check = false
      )

    assert(res.exitCode == 0)

    val Completion = """final COMPREPLY\[\d+\]: '(\S+).*'""".r
    val completionResults = collection.mutable.Map.empty[String, Seq[String]]

    val lines = os.read(log).linesIterator
    var line = ""
    def nextLine() =
      if lines.hasNext then
        line = lines.next()
      else
        line = null

    nextLine()
    while line != null do
      if line.startsWith("starting completion for '") then
        val cmdLine = line.substring("starting completion for '".length, line.length - 1)
        nextLine()
        val completions = collection.mutable.ArrayBuffer.empty[String]
        while line != null && !line.startsWith("starting completion for '") do
          line match
            case Completion(comp) =>
              completions += comp
            case _ => // ignore
          nextLine()
        completionResults += cmdLine -> completions.toSeq
      else
        nextLine()

    println(tmp)

    for (test, expected) <- tests do
      assert(completionResults.contains(test))
      val actual = completionResults(test)
      assert(expected.toSet == actual.toSet)

  val tests = Tests:
    val base = os.Path(sys.env("MILL_WORKSPACE_ROOT"))
    test("completion1"):
      os.write(os.pwd / "dummy1.txt", "dummy")
      os.write(os.pwd / "dummy2.txt", "dummy")
      os.write(os.pwd / "dummy3.txt", "dummy")

      testCompletions(base / "out" / "examples" / "completion1" / "assembly.dest" / "out.jar")(
        "app --" -> Seq(
          "--string-param",
          "--path-param",
          "--custom-param",
          "--help",
          "--completion",
          "--color"
        ),
        "app --h" -> Seq("--help"),
        "app --string-param " -> Seq(),
        // "app --path-param " -> Seq("dummy"), // TODO paths somehow not working in test harness
        "app --custom-param " -> Seq("foo", "bar", "baz"),
        "app --custom-param b" -> Seq("bar", "baz"),
        "app --custom-param ba" -> Seq("bar", "baz"),
        "app --custom-param bar" -> Seq("bar"),
        "app --custom-param bar " -> Seq()
      )

    test("completion2"):

      testCompletions(base / "out" / "examples" / "completion2" / "assembly.dest" / "out.jar")(
        "app --" -> Seq(
          "--set",
          "--help",
          "--completion",
          "--color"
        ),
        "app --set 1 subcmd1 --item b" -> Seq("bar", "baz"),
        "app --set 2 subcmd1 --item b" -> Seq("banana", "blueberry"),
        "app " -> Seq("subcmd1", "subcmd2"),
        "app subcmd2 " -> Seq("alpha", "beta", "gamma"),
        "app subcmd2 a" -> Seq("alpha"),
        "app subcmd2 alpha " -> Seq("delta", "epsilon"),
        "app subcmd2 alpha d" -> Seq("delta"),
        "app subcmd2 alpha delta " -> Seq("delta", "epsilon"),
        "app subcmd2 alpha delta epsilon -" -> Seq("--named", "--help"),
        "app subcmd2 alpha delta epsilon --named " -> Seq("one", "two", "three")
      )

