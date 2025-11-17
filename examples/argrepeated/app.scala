// By default, parameters can only be specified once on the command line. If you
// want parameters to be repeatable multiple times, you can use the `repeats`
// option in the `@clip.arg` annotation.
//
// Repeated parameters must be of type `Iterable[T]`.
//snippet:start
@clip.command()
def app(
    @clip.arg("--name", repeats = true)
    names: Seq[String],
    @clip.arg("extra", repeats = true)
    extra: Seq[String]
): Unit =
  println(s"names: ${names.mkString(", ")}, extra: ${extra.mkString(", ")}")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

/* usage snippet
$ ./app --name=Alice Alpha Bravo --name=Bob --name=Charlie Charlie
names: Alice, Bob, Charlie, extra: Alpha, Bravo, Charlie
*/

// Arguments after a literal `--` are always treated as positional, even if they
// start with a `-` or `--`.

/* usage snippet
$ ./app --name=Alice A B --name=Bob --name=Charlie Charlie --Delta
unknown argument: '--Delta'
run with --help for more information
*/

/* usage snippet
$ ./app --name=Alice A B --name=Bob --name=Charlie Charlie -- --Delta
names: Alice, Bob, Charlie, extra: A, B, Charlie, --Delta
*/


