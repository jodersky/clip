import utest.*

import clip.dispatch.Command
import clip.dispatch.Param
import clip.dispatch.InvocationResult

object DispatchTest extends TestSuite:

  def testInvoke(cmd: clip.dispatch.Command[Any, ?], args: Iterable[String]) =
    clip.dispatch.invokeRaw(cmd, (), args)._2

  val tests = Tests:
    test("simple command"):
      val cmd = Command(
        name = "example",
        help = "An example group",
        listChildren = () => List.empty,
        getChild = _ => None,
        params = List(
          Param(List("--count", "-c"))
        ),
        invoke = (_, _, _) => InvocationResult.Success("ok"),
        terminal = true,
      )

      assert(testInvoke(cmd, List()).isSuccess)
      assert(testInvoke(cmd, List("onetoomany")) == InvocationResult.ParamError(
        unknown = Seq("onetoomany")
      ))

      assert(testInvoke(cmd, List("--count")).isSuccess)
      assert(testInvoke(cmd, List("--foo")) == InvocationResult.ParamError(
        unknown = Seq("--foo")))

    test("nested command")
      val subcmd1 = Command(
        name = "sub",
        invoke = (_, ctx: String, _) => InvocationResult.Success(ctx + " sub1"),
      )
      val subcmd2 = Command(
        name = "sub2",
        invoke = (_, ctx: String, _) => InvocationResult.Success(ctx + " sub2")
      )

      val cmd = Command(
        name = "example",
        listChildren = () => List(subcmd1, subcmd2),
        getChild = name => if name == "sub1" then Some(subcmd1) else if name == "sub2" then Some(subcmd2) else None,
        invoke = (_, _, _) => InvocationResult.Success("parent"),
        terminal = false
      )

      assert(testInvoke(cmd, List()) == InvocationResult.MissingCommand())
      assert(testInvoke(cmd, List("sub1")) == InvocationResult.Success("parent sub1"))
      assert(testInvoke(cmd, List("sub2")) == InvocationResult.Success("parent sub2"))
      assert(testInvoke(cmd, List("sub1", "extra")) == InvocationResult.ParamError(
        unknown = Seq("extra")
      ))
      assert(testInvoke(cmd, List("unknown")) == InvocationResult.UnknownCommand("unknown"))
