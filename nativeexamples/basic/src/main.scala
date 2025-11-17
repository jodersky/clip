@clip.command()
def main(arg1: String, arg2: Int = 42) =
  clip.echo(s"arg1: $arg1, arg2: $arg2")

def main(args: Array[String]) = clip.main(this, args)
