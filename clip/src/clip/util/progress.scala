package clip.util.progress

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import scala.compiletime.uninitialized

trait StatusHandle:
  def update(text: String): Unit

/** Render multiple, independently updatable status lines to a terminal.
  *
  * Call `start()` to begin rendering on a background thread, obtain one
  * `StatusHandle` per line via `addLine()`, and have concurrent tasks call
  * `update()` on their handle whenever their status changes. Call `stop()`
  * once all tasks are done to stop rendering and restore the terminal.
  */
final class MultiStatus(
    out: java.io.PrintStream = System.out
):
  private val lines = new CopyOnWriteArrayList[String]()
  private var linesOnScreen = 0
  private val running = new AtomicBoolean(false)
  private var renderThread: Thread = uninitialized
  private var shutdownHook: Thread = uninitialized

  def addLine(initial: String = ""): StatusHandle =
    val id = lines.size()
    lines.add(initial)
    text => lines.set(id, text)

  def start(refreshIntervalMs: Long = 80): Unit =
    if running.compareAndSet(false, true) then
      out.print("\u001b[?25l") // hide cursor
      out.flush()

      shutdownHook = new Thread(() =>
        if running.compareAndSet(true, false) then finish()
      )
      Runtime.getRuntime().addShutdownHook(shutdownHook)

      val renderLoop: Runnable = () =>
        while running.get() do
          render()
          Thread.sleep(refreshIntervalMs)
      renderThread = new Thread(renderLoop, "multi-status-render")
      renderThread.setDaemon(true)
      renderThread.start()

  def stop(): Unit =
    if running.compareAndSet(true, false) then
      finish()
      try Runtime.getRuntime().removeShutdownHook(shutdownHook)
      catch case _: IllegalStateException => () // JVM already shutting down

  // Stops the render thread and leaves the terminal in a clean state: cursor
  // positioned right below the last status line (not stranded mid-block) and
  // visible again. Shared by stop() and the shutdown hook, so a Ctrl+C that
  // lands mid-render can't leave the cursor sitting in the middle of the
  // status block.
  private def finish(): Unit =
    renderThread.join()
    render()
    out.print("\u001b[?25h") // show cursor
    out.flush()

  private def render(): Unit =
    if linesOnScreen > 0 then out.print(s"\u001b[${linesOnScreen}A")
    val it = lines.iterator()
    while it.hasNext do
      out.print("\r\u001b[2K")
      out.println(it.next())
    linesOnScreen = lines.size()
    out.flush()
