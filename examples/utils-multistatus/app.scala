// `clip.progressbar` is great for tracking a single, linear task. When you have
// several independent tasks running concurrently, use
// `clip.MultiStatus` instead: each task gets its own line via a
// `StatusHandle`, which it can update from any thread. A background thread
// repaints all lines together at a fixed interval, so concurrent updates from
// multiple threads never interleave or corrupt the terminal output.

//snippet:start
@clip.command()
def app() =
  val status = clip.MultiStatus()
  status.start()

  val names = List("alpha", "beta", "gamma", "delta")
  val threads = names.map: name =>
    val handle = status.addLine(s"$name: waiting")
    new Thread(() =>
      val rnd = new scala.util.Random()
      var pct = 0
      while pct < 100 do
        Thread.sleep(50 + rnd.nextInt(150))
        pct = (pct + 1 + rnd.nextInt(10)).min(100)
        val filled = pct / 5
        val bar = "#" * filled + "-" * (20 - filled)
        handle.update(f"$name%-6s [$bar] $pct%3d%%")
      handle.update(f"$name%-6s done")
    )

  threads.foreach(_.start())
  threads.foreach(_.join())
  status.stop()
//snippet:end

def main(args: Array[String]): Unit = clip.main(this, args)

// ![multistatus](examples/utils-multistatus/multistatus.gif)
