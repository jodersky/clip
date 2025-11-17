// The previous example showed how to define subcommands, but they were all
// isolated and had nothing in common. In practice, you often want to group
// related commands together, possibly sharing some common parameters or state.
//
// You can do this by:
// 1. defining parameters on the group itself
// 2. having the group function return an arbitrary object of your choice,
// 3. and then having the subcommands accept that object by marking it with a
//    `@clip.context` annotation.
//
// Example:
//snippet:start
case class AppConfig(verbosity: Int)

@clip.group()
def app(verbosity: Int = 0): AppConfig =
  AppConfig(verbosity)

@clip.command()
def update(
    @clip.context config: AppConfig
) =
  if config.verbosity > 0 then
    clip.echo(s"[verbose] Updating items with verbosity ${config.verbosity}")
  else
    clip.echo(s"Updating items")

@clip.command()
def list(
  @clip.context config: AppConfig,
  limit: Int = 10
) =
  if config.verbosity > 0 then
    clip.echo(s"[verbose] Listing items (limiting to ${limit}) with verbosity ${config.verbosity}")
  else
    clip.echo(s"Listing items (limiting to ${limit})")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end
