// Clip encourages the use of subcommands for structuring larger CLI
// applications.

//snippet:start
// We need to define a group to hold the subcommands. For now this is empty,
// but it could also contain shared parameters.
@clip.group()
def app(): Unit =
  ()

// Subcommands are defined as usual commands in the same file
@clip.command()
def update() =
  clip.echo(s"Updating items")

// Each subcommand can have its own parameter list
@clip.command()
def list(limit: Int = 10) =
  clip.echo(s"Listing items (limiting to ${limit})")

def main(args: Array[String]): Unit = clip.main(this, args)
//snippet:end

/* usage snippet
$ ./app update
Updating items
*/

/* usage snippet
$ ./app list
Listing items (limiting to 10)
*/

/* usage snippet
$ ./app list --limit 5
Listing items (limiting to 5)
*/

// Parameter lists are separate, so if you try to pass --limit to update, it
// will fail
/* usage snippet
$ ./app update --limit 5
unknown argument: '--limit'
run with --help for more information
*/

// If the command doesn't exist but is similar to others, they will be suggested
// automatically. Similarity detection is based on the Jaro-Winkler distance algorithm
/* usage snippet
$ ./app l
unknown command: 'l'
run with --help for more information
similar commands:
  list
*/
