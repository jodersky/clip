// You might have noticed that all commands have a `--help` parameter available
// which is not explicitly defined in the annotated method (there's no `help`
// parameter in the method signature). Furthermore, when you pass in `--help`,
// the command changes its behavior: rather than parsing arguments the normal
// way, `--help` causes the command to print a message and exit.
//
// This is an example of a so-called *eager parameter*. Eager parameters are
// parameters that are parsed and handled before the main command logic is
// executed, and they can alter the behavior of the command itself.
//
// Other examples of eager parameters include the `--color` parameter, which as
// a side-effect sets up colored output for the command, and the `--completion`
// parameter, which generates shell completion scripts for the command.
//
// You can define your own eager parameters and add them to your commands. Let's
// look at an example, where we define a `--version` eager parameter that causes
// the command to print its version and exit.

//snippet:start
// an eager parameter essentially consists of two parts:
// 1. a parameter definition, which defines the name, help message, and other
//    properties of the parameter
// 2. an invocation function, which is always called before the main command
//    logic. This function can perform side-effects (such as printing to the
//    console) and return either a result (to stop further processing) or
//    indicate that processing should continue as normal.
val version = clip.dispatch.EagerParam(
  // the parameter definition
  param = clip.dispatch.Param(
    names = Seq("--version", "-v"),
    argName = None,
    help = "Show application version"
  ),
  // the invocation function: chain is the command chain leading to this
  // command, ctx is the command context, and args is the map of parsed
  // arguments
  invoke = (chain, ctx, args) =>
    val versionRequested = args("--version").nonEmpty
    if versionRequested then
      System.out.println("App version 1.0.0")
      // return a result to stop further processing
      clip.dispatch.EagerResult.Return(
        clip.dispatch.InvocationResult.Success(())
      )
    else
      // indicate that processing should continue as normal
      clip.dispatch.EagerResult.Continue
)

// we can now add this eager parameter to our command using the `eagers`
// parameter of the `@clip.command` annotation
@clip.command(
  eagers = Seq(version)
)
def app(): Unit =
  println("Hello from the app command!")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// Now, when we run our application with the `--version` flag, we see the
// version information printed to the console, and the main command logic is not
// executed.

/* usage snippet
$ ./app --version
App version 1.0.0
*/

// If we run the application without the `--version` flag,
// the main command logic is executed as normal.

/* usage snippet
$ ./app
Hello from the app command!
*/

// Note that printing a version is such a common use-case for eager parameters
// that clip already has a built-in eager parameter for this purpose: if you
// define a `version` parameter in your `@clip.command` or `@clip.group`
// annotation, for example `@clip.command(version="1.0.0")` you'll automatically
// get a `--version` eager parameter that prints the version for you!
