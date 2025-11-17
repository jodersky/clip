// All commands in clip have a `--help` parameter which can be passed to print
// the help message for the command.
//
// The help message is generated automatically based on the command's
// parameters, their descriptions, and the command's documentation.
//
// // todo
//
// In addition to the `--help` parameter, you might have noticed that commands
// also have other parameters available, such as `--color` or `--completion`.
// These are known as *eager* parameters, since their values are checked before
// the command is executed, and they can be used to modify the behavior of the
// command itself.
//
// All commands have the following eager parameters:
// - `--help`: prints the help message for the command
//
// The top-level command (one which is not a subcommand of another command) will
// also have the following eager parameters:
// - `--color`: enables or disables colored output
// - `--completion`: prints the completion script for the command
// - `--version`: prints the version of the command (only if a version is defined)
//
// You can also define your own eager parameters for your commands,
// - locally, by adding them to the @command() annotation
// - globally, by defining them in your own API trait
//
// // TODO

//> using dep io.crashbox::clip::0.1.0
