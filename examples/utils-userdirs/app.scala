@clip.command()
def app() =

  // The `clip.userdirs` utility class provides a convenient way to access
  // user-specific directories for configuration, cache, data, state, and
  // runtime. It is designed for command line applications that run in a user
  // context, not a system context (e.g., a daemon).
  //
  // It is a good idea to use this utility class to ensure that your application
  // respects the user's preferences for where to store files.
  //
  // As of now it uses the XDG Base Directory Specification to determine the
  // appropriate directories, and it is currently only implemented for Linux.
  //

  // A directory where the application can store its configuration files.
  //
  // `~/.config/myapp` is the default location
  //snippet:start
  clip.userdirs("myapp").config
  //snippet:end

  // A directory where the application can store cached files. These files can
  // be recreated or downloaded again if deleted.
  //
  // `~/.cache/myapp` is the default location
  //snippet:start
  clip.userdirs("myapp").cache
  //snippet:end

  // A collection of preference-ordered directories where the application can
  // find data files. Data files are typically static files that the application
  // needs to function, but don't contain the application's state (e.g., static
  // media files).
  //
  // `~/.local/share/myapp` is the default location, followed by
  // `/usr/local/share/myapp` and `/usr/share/myapp`.
  //snippet:start
  clip.userdirs("myapp").data
  //snippet:end

  // The data collection always has at least one element.
  //snippet:start
  clip.userdirs("myapp").data.head
  //snippet:end

  // A directory where the application can store its state. This is typically
  // used for files that should persist between runs, such as database files.
  //
  // `~/.local/state/myapp` is the default location
  //snippet:start
  clip.userdirs("myapp").state
  //snippet:end

  // A directory where the application can store special files such as sockets
  // and named pipes. This directory is typically cleared on reboot.
  //
  // `/run/$UID/myapp` is the default location
  //snippet:start
  clip.userdirs("myapp").runtime
  //snippet:end


def main(args: Array[String]): Unit = clip.main(this, args)
