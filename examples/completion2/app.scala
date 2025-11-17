// Sometimes, you'd like completions to depend on other parameters. For example,
// a prior parameter might change some configuration that affects the valid
// values for a later parameter.
//
// In clip, a dynamic completer can access the command context, which can be
// used to share information between parameters.
//
// Let's look at an example where the available completions for an `--item`
// parameter depend on the value of another `--set` parameter.

//snippet:start
case class Context(items: Set[String])

@clip.group()
def app(set: Int = 1) =
  if set == 1 then
    Context(Set("foo", "bar", "baz"))
  else if set == 2 then
    Context(Set("apple", "banana", "blueberry"))
  else
    clip.abort(message = "unknown set: " + set)

@clip.command()
def subcmd(
  @clip.context ctx: Context,
  @clip.arg("--item", completer = clip.Completer.Dynamic(
    (ctx, partial) =>
      ctx.asInstanceOf[Context].items.filter(_.startsWith(partial))
  )) item: String
) =
  clip.echo(s"Selected item: $item")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// ```
// $ ./app --set 1 subcmd --item b<TAB>
// bar  baz
// ```

// ```
// $ ./app --set 2 subcmd --item b<TAB>
// banana  blueberry
// ```
