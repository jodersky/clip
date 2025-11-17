// You can display a progress bar for long-running tasks using clip.progressbar.
//
// Use `clip.progressbar` to set up basic progress bar parameters, then `.wrap`
// it around a collection or a `geny.Readable` to track progress as you iterate
// over the items.

//snippet:start
@clip.command()
def app() =
  val items = 0 until 1000
  val progress = clip.progressbar("Processing").wrap(items)
  for item <- progress do
    // Simulate some work
    Thread.sleep(5)

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// ![progress bar](rexamples/utils-progress/progress.gif)
