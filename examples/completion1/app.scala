// Any command in clip automatically supports shell completion. Completion of
// arguments is based on the parameter types. For example, string parameters
// have no completion, while `os.Path` parameters can be completed using the
// filesystem.
//
// You can also define custom completers for parameters, which can be useful if
// acceptable values are dynamic (for example, the command needs to query a
// server to get the list of valid options).
//
// Let's look at an example application that demonstrates different types of
// completions, including a custom dynamic completer.

//snippet:start
@clip.command(
  name = "app",
  help = "A simple example application using clip"
)
def app(
  @clip.arg("--string-param", help = "string parameter has no completion")
  stringParam: String = "default",
  @clip.arg("--path-param", help = "paths can be completed with the filesystem")
  pathParam: os.Path = os.pwd / "default.txt",
  @clip.arg(
    "--custom-param", help = "custom parameter with dynamic completion",
    completer = clip.Completer.Dynamic(
      (_, partial) =>
        Set("foo", "bar", "baz").filter(_.startsWith(partial))
    )
  )
  customParam: String = "default"
) =
  clip.echo(s"String parameter: $stringParam")
  clip.echo(s"Path parameter: $pathParam")
  clip.echo(s"Custom parameter: $customParam")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// To get shell completion working for your app, you need to generate a
// completion script and source it in your shell:
//
// 1. Generate the completion script and save it to a file
//
//    ```
//    ./app --completion > app-completion.sh
//    ```
//
// 2. Source the completion script in your shell
//
//    ```
//    source app-completion.sh
//    ```
//
// Now you can use tab-completion for your command! Here are some examples:

// ```
// $ ./app --string-param <TAB>
// ```

// ```
// $ ./app --path-param <TAB>
// file1.txt  file2.txt  folder1/  folder2/  etc...
// ```

// ```
// $ ./app --custom-param b<TAB>
// bar  baz
// ```

// In practice, you would typically add the sourcing of the completion script to
// your shell's startup file (e.g., `.bashrc`).
