package clip.dispatch

case class EagerParam(
    param: Param,
    invoke: InvokeEager[Any]
)

object StandardEagerParams:

  def help() = EagerParam(
    Param(
      names = Seq("--help"),
      help = "Show help message",
      argName = None,
      repeats = false
    ),
    (chain, ctx, args) =>
      val helpRequested = args("--help").nonEmpty
      if helpRequested then
        clip.dispatch.help2(
          System.out,
          chain.last,
          chain.map(_.name).toSeq
        )
        EagerResult.Return(InvocationResult.Success(()))
      else EagerResult.Continue
  )

  def version(versionString: String) = EagerParam(
    Param(
      names = Seq("--version"),
      help = "Show version information",
      argName = None,
      repeats = false
    ),
    (chain, ctx, args) =>
      val versionRequested = args("--version").nonEmpty
      if versionRequested then
        System.out.println(versionString)
        EagerResult.Return(InvocationResult.Success(()))
      else EagerResult.Continue
  )

  def completion() = EagerParam(
    Param(
      names = Seq("--completion"),
      help = "Generate shell completion script",
      argName = Some("shell"),
      repeats = false
    ),
    (chain, ctx, args) =>
      val completionArgs = args("--completion")
      if completionArgs.nonEmpty then
        val shellArg = completionArgs.headOption.flatMap(_.value)
        val (shell, aliases) = shellArg match
          case Some(s) =>
            val parts = s.split(",", -1)
            if parts.length == 0 then
              ("bash", Seq(chain.head.name))
            else if parts.length == 1 then
              (parts.head, Seq(chain.head.name))
            else
              (parts.head, parts.tail.toSeq)
          case None => ("bash", Seq(chain.head.name))

        shell match
          case "bash" =>
            clip.dispatch.generateCompletionRecursively(
              aliases,
              System.out,
              chain.last
            )
            EagerResult.Return(InvocationResult.Success(()))
          case other =>
            System.err.println(s"unsupported shell for completion: '$other'")
            EagerResult.Return(
              InvocationResult.ParamError(invalid =
                Seq(("--completion", "unsupported shell: " + other))
              )
            )
      else EagerResult.Continue
  )

  def color() = EagerParam(
    Param(
      names = Seq("--color"),
      help = "Set color mode (auto, always, never)",
      argName = Some("mode"),
      repeats = false,
      completer = clip.Completer.OneOf(Seq("auto", "always", "never"))
    ),
    (chain, ctx, args) =>
      val colorArgs = args("--color")
      if colorArgs.nonEmpty then
        val modeStrOpt = colorArgs.headOption.flatMap(_.value)
        modeStrOpt match
          case Some("auto") =>
            clip.util.termui.ColorMode
              .setGlobalColorMode(clip.util.termui.ColorMode.Auto)
            EagerResult.Continue
          case Some("always") =>
            clip.util.termui.ColorMode
              .setGlobalColorMode(clip.util.termui.ColorMode.Always)
            EagerResult.Continue
          case Some("never") =>
            clip.util.termui.ColorMode
              .setGlobalColorMode(clip.util.termui.ColorMode.Never)
            EagerResult.Continue
          case Some(other) =>
            EagerResult.Return(
              InvocationResult.ParamError(invalid =
                Seq(("--color", "invalid color mode: " + other))
              )
            )
          case None =>
            EagerResult.Return(
              InvocationResult.ParamError(invalid =
                Seq(("--color", "missing color mode"))
              )
            )
      else EagerResult.Continue
  )
