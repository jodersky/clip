// The default `MultiStatus` repaints by moving the cursor up and redrawing,
// which assumes nothing else writes to the terminal in the meantime. Pass
// `scroll = true` to `start()` to instead reserve a scroll region at the
// bottom of the terminal for the status lines (the same trick tools like
// `apt` use for their progress bar). Regular output written elsewhere in
// your program, such as `println`, then scrolls normally above the status
// area instead of interleaving with it or being overwritten by repaints.

//snippet:start
@clip.command()
def app() =
  val status = clip.MultiStatus()
  status.start(scroll = true)

  val names = List("alpha", "beta", "gamma", "delta")
  val threads = names.map: name =>
    val handle = status.addLine(s"$name: waiting")
    new Thread(() =>
      val rnd = new scala.util.Random()
      var pct = 0
      while pct < 100 do
        // this line is printed from user code, not the status handle, yet it
        // won't clobber or get clobbered by the status lines below it
        println("\u001B[38;5;248mupdating " + name + "\u001B[0m")
        Thread.sleep(50 + rnd.nextInt(150))
        pct = (pct + 1 + rnd.nextInt(10)).min(100)
        val filled = pct / 5
        val bar = "#" * filled + "-" * (20 - filled)
        handle.update(f"$name%-6s [$bar] $pct%3d%%")

      println(s"$name finished")
      handle.update(f"$name%-6s done")
    )

  threads.foreach(_.start())
  threads.foreach(_.join())
  status.stop()
//snippet:end

def main(args: Array[String]): Unit = clip.main(this, args)

// ![scroll region example](examples/utils-multistatus-scroll/scroll.gif)

// Note that multi-status bars restore terminal state when interrupted. This
// means that you should never see a partially-rendered status bar or clobbered
// terminal output when pressing Ctrl+C.

// ![multi status interrupt example](examples/utils-multistatus-scroll/interrupt.gif)
