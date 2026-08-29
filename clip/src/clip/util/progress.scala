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

  // When scroll mode is enabled, a terminal scroll region (DECSTBM) is used
  // instead of cursor-up repainting, so status lines live in a fixed area at
  // the bottom of the screen and ordinary output (e.g. a user's println)
  // scrolls independently above it. reservedRows tracks how many bottom rows
  // are currently carved out for status, so the region can grow as lines are
  // added.
  private var scroll = false
  private var reservedRows = 0

  def addLine(initial: String = ""): StatusHandle =
    val id = lines.size()
    lines.add(initial)
    text => lines.set(id, text)

  /** @param scroll
    *   If true, reserve a scroll region at the bottom of the terminal for
    *   status lines (similar to apt's progress bar), so that output written
    *   by user code (e.g. `println`) scrolls independently above it instead
    *   of interleaving with or being overwritten by status repaints.
    *   Requires a terminal that supports ANSI scroll regions.
    */
  def start(refreshIntervalMs: Long = 80, scroll: Boolean = false): Unit =
    if running.compareAndSet(false, true) then
      this.scroll = scroll
      reservedRows = 0
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
    if scroll && reservedRows > 0 then
      val (rows, _) = clip.util.term.sizeOrDefault()
      out.print("\u001b[r") // reset scroll region to the full screen
      out.print(s"\u001b[$rows;1H\n") // land below the status area
      reservedRows = 0
    out.print("\u001b[?25h") // show cursor
    out.flush()

  private def render(): Unit =
    if scroll then renderScroll() else renderPlain()

  private def renderPlain(): Unit =
    if linesOnScreen > 0 then out.print(s"\u001b[${linesOnScreen}A")
    val it = lines.iterator()
    while it.hasNext do
      out.print("\r\u001b[2K")
      out.println(it.next())
    linesOnScreen = lines.size()
    out.flush()

  // Renders status lines into a fixed region at the bottom of the screen
  // using absolute cursor addressing, leaving the region above (and the
  // caller's cursor position) untouched. If the number of lines has grown
  // since the last render, the scroll region is widened first to make room.
  private def renderScroll(): Unit =
    val n = lines.size()
    if n == 0 then return

    val (rows, _) = clip.util.term.sizeOrDefault()
    if n != reservedRows then
      val growth = n - reservedRows
      if growth > 0 then out.print("\n" * growth) // make room at the bottom
      val bottom = math.max(1, rows - n)
      out.print(s"\u001b[1;${bottom}r") // restrict scrolling to rows above
      out.print(s"\u001b[$bottom;1H") // return cursor to top of user area
      reservedRows = n

    out.print("\u001b7") // save cursor
    var row = rows - n + 1
    val it = lines.iterator()
    while it.hasNext do
      out.print(s"\u001b[$row;1H\u001b[2K${it.next()}")
      row += 1
    out.print("\u001b8") // restore cursor
    out.flush()
