@clip.command()
def app() =
  // In case you need to open a URL or file from your application, you can use
  // `clip.launch`. It will open the URL or file using the user's preferred
  // application for that file type.

  // For example, to open a URL in the web browser:
  //snippet:start
  clip.launch("https://www.example.com")
  //snippet:end

  // Or to open a file in the default text editor:
  //snippet:start
  clip.launch("README.md")
  //snippet:end

  // As of now, `clip.launch` only works on Linux, on systems which support the
  // xdg-open command.

def main(args: Array[String]): Unit = clip.main(this, args)
