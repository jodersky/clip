// Sometimes, a named parameter should be usable both as a flag and with an
// argument. For example, `--jobs` could mean "use a sensible default number of
// jobs", while `--jobs=4` would mean "use exactly 4 jobs".
//
// Clip supports this with parameters of a union type of `Boolean` and the type
// of the argument:

//snippet:start
@clip.command()
def app(
  @clip.arg(
    "--jobs",
    aliases = Seq("-j"),
    help = "run in parallel, with an optional number of jobs",
    completer = clip.Completer.OneOf(Seq("1", "2", "4", "8"))
  )
  jobs: Boolean | Int = false,
  @clip.arg("target", completer = clip.Completer.OneOf(Seq("build", "test")))
  target: String
): Unit =
  jobs match
    case false     => println(s"$target: sequentially")
    case true      => println(s"$target: in parallel, with default jobs")
    case n: Int    => println(s"$target: in parallel, with $n jobs")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// - not specifying the parameter results in its default value (`false`)
/* usage snippet
$ ./app build
build: sequentially
*/

// - specifying the parameter without an argument results in `true`
/* usage snippet
$ ./app --jobs build
build: in parallel, with default jobs
*/

// - specifying the parameter with an argument results in the argument's value
/* usage snippet
$ ./app --jobs=4 build
build: in parallel, with 4 jobs
*/

// Since the argument is optional, it must be given with an `=`. Otherwise, it
// would be ambiguous whether the next word is the parameter's argument or a
// positional parameter. Hence, the following treats `4` as the target:
/* usage snippet
$ ./app --jobs 4
4: in parallel, with default jobs
*/

// The help message shows that the argument is optional:
/* usage snippet
$ ./app --help
Usage: app [options] target

Options:
  --color <mode>          Set color mode (auto, always, never)
  --completion <shell>    Generate shell completion script
  --help                  Show help message
  -j, --jobs[=<integer>]  run in parallel, with an optional number of jobs
Positional arguments:
  target
*/
