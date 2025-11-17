// Clip has various utilities to read user input from the terminal.
//
// Note: when building CLI applications, prefer using command line arguments for
// input whenever possible. Reading from the terminal should be reserved for
// cases where you only ever want your application to be used interactively.
//snippet:start
@clip.command()
def app() =

  // You can use clip.prompt to read user input from the terminal. If the
  // session is non-interactive (e.g., input is redirected from a file or pipe),
  // clip.prompt will use the default, or throw an error if no default is
  // provided.
  val name: String = clip.prompt[String]("Enter your name", default = "Guest")

  // It can read various types, as long as there is a Reader available, just
  // like for command line parameters.
  // If the user enters invalid input, they will be prompted again.
  val age: Int = clip.prompt[Int]("Enter your age")

  // You can also hide input for sensitive data like passwords. Setting password = true
  // will disable echoing of input characters to the terminal.
  val password: String = clip.prompt[String]("Enter your password", password = true)

  // For yes/no questions, you can use clip.confirm, which returns a Boolean.
  // It accepts various forms of yes/no input (y/n, yes/no).
  val confirm: Boolean = clip.confirm("Do you want to proceed?")

  if confirm then
    clip.echo(s"Hello, $name! You are $age years old.")
    clip.echo(s"Your password is ${password}")
  else
    clip.echo("Operation cancelled.")
    clip.abort(2)

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

// ```
// $ ./app
// Enter your name [default: Guest]: Alice
// Enter your age: a
// Invalid input: 'a' is not an integral number
// Enter your age: 30
// Enter your password: [input hidden]
// Do you want to proceed? (y/n): y
// Hello, Alice! You are 30 years old.
// Your password is mysecretpassword
// ```
