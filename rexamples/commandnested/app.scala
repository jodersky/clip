
// We saw previously how to define subcommands in the same file as the
// application's main. But what if you want to organize your commands in multiple
// files, or have multiple levels of nesting?

// In this case you need to define your subcommands in separate objects or
// packages, and then refer to them from the group definitions.
//snippet:start
package main:
  case class AppConfig(verbosity: Int)

  @clip.group(
    help = "A CLI application demonstrating nested subcommands",
    subcommands = sub2.cmds ++ Seq(sub3.cmd) // notice how we refer to commands defined in another package, these will be used in addition to any commands defined here
  )
  def app(verbosity: Int = 10) = AppConfig(verbosity)

  @clip.command()
  def sub1() =
    clip.echo(s"Subcommand 1 executed")

  def main(args: Array[String]): Unit = clip.main(this, args)

package sub2:

  @clip.command()
  def sub2() =
    clip.echo(s"Subcommand 2 executed")

  @clip.command()
  def sub3() =
    clip.echo(s"Subcommand 3 executed")

  // helper to get all commands defined in this package as subcommands
  val cmds = clip.subcommandsFor(this)

// this contains nested subcommands
package sub3:
  case class NestedConfig(app: main.AppConfig)

  @clip.group()
  def sub3(@clip.context config: main.AppConfig) =
    NestedConfig(config)

  @clip.command()
  def nested1(@clip.context config: NestedConfig) =
    clip.echo(s"Nested subcommand 1 executed with verbosity ${config.app.verbosity}")

  @clip.command()
  def nested2() =
    clip.echo(s"Nested subcommand 2 executed")

  // helper to make a new command with a group in this package
  val cmd = clip.commandFor(this)
//snippet:end
