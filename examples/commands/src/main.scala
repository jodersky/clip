package app

@clip.group(
  help = "A CLI application demonstrating subcommands",
  subcommands = Seq(op.cmds)
)
def app(): Any = ()

@clip.command()
def version() = clip.echo(s"v1000")

def main(args: Array[String]): Unit = clip.main(this, args)
