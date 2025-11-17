// At minimum, a clip application consists of a method annotated with
// `@clip.command()`, and a main method that calls `clip.main`.
//snippet:start
@clip.command()
def app(
  count: Int = 5,
  name1: String,
  name2: String,
  flag: Boolean = false,
): Unit =
  println(s"count: ${count}, flag: ${flag}, name1: ${name1}, name2: ${name2}")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// Command line parameters are automatically defined based on the method
// parameters, following these rules:
//
// - Parameters with default values become named parameters on the command line.
//   These start with `-` or `--`, and can be specified in any order.
//
//   For example, the parameter `count: Int = 5` becomes the named parameter
//   `--count <integer>`, which defaults to `5` if not specified.
//
// - Parameters without default values become positional parameters, which must
//   be specified in the order they are defined.
//
//   For example, the parameters `name1: String` and `name2: String` become the
//   positional parameters `name1` and `name2`.
//
// - Parameters of type `Boolean` become flags, which are named parameters that
//   don't take any argument. Specifying the flag sets the parameter to `true`.
//
//   For example, the parameter `flag: Boolean = false` becomes the flag
//   `--flag`, which defaults to `false` if not specified, and becomes `true` if
//   specified.

// Here is what this looks like in practice:

/* usage snippet
$ ./app Alice --count 3 Bob --flag
count: 3, flag: true, name1: Alice, name2: Bob
*/

/* usage snippet
$ ./app Alice Bob
count: 5, flag: false, name1: Alice, name2: Bob
*/

// You can also set named parameters with `=`
/* usage snippet
$ ./app Alice --count=2 Bob
count: 2, flag: false, name1: Alice, name2: Bob
*/

// Clip automatically show errors for missing required arguments, too many
// arguments, and invalid argument.

/* usage snippet
$ ./app Alice --flag
missing required argument: 'name2'
run with --help for more information
*/

/* usage snippet
$ ./app Alice Bob Charlie
unknown argument: 'Charlie'
run with --help for more information
*/

/* usage snippet
$ ./app --count=oops Alice Bob
invalid value for '--count': 'oops' is not an integral number
run with --help for more information
*/

// In case of argument errors, clip exits with exit code 2.
