package clip.util

/** Utility class for getting *user* directories for common types of application
  * data.
  *
  * Use this only if your command line app is run in a *user* context, NOT a
  * *system* context (e.g., a daemon).
  *
  * TODO: add Windows and MacOS support
  */
class userdirs(appName: String):

  /** Directory containing configuration files. These files are typically
    * human-editable, and should generally be treated as read-only by the
    * application (except if the application is specifically manipulating its
    * own configuration).
    *
    * You can think of it as the equivalent of `/etc/<appName>` in a system-wide
    * installation, but for user-specific configuration files.
    */
  val config: os.Path =
    sys.env.get("XDG_CONFIG_HOME") match
      case Some(path) => os.Path(path) / appName
      case None       => os.home / ".config" / appName

  /** Directory for cached data files. These files can be regenerated or
    * downloaded again if deleted.
    *
    * You can think of it as the equivalent of `/var/cache/<appName>` in a
    * system-wide installation, but for user-specific cached data files.
    */
  val cache: os.Path =
    sys.env.get("XDG_CACHE_HOME") match
      case Some(path) => os.Path(path) / appName
      case None       => os.home / ".cache" / appName

  /** Preference-ordered directories for data files.
    *
    * Data files are files that are necessary for the application to function,
    * but don't contain the application's state (e.g., static images, templates,
    * etc.).
    *
    * You can think of it as the equivalent of `/usr/share/<appName>` in a
    * system-wide installation, but for user-specific data files.
    */
  val data: Seq[os.Path] =
    val home = sys.env.get("XDG_DATA_HOME") match
      case Some(path) => os.Path(path) / appName
      case None       => os.home / ".local" / "share" / appName
    val other =
      sys.env.get("XDG_DATA_DIRS") match
        case Some(paths) =>
          paths.split(":").map(p => os.Path(p) / appName).toSeq
        case None =>
          Seq(
            os.root / "usr" / "local" / "share" / appName,
            os.root / "usr" / "share" / appName
          )
    home +: other

  /** Directory for state files. This is where the application can store its
    * state that should persist between runs, such as database files.
    *
    * You can think of it as the equivalent of `/var/lib/<appName>` in a
    * system-wide installation, but for user-specific state files.
    */
  val state: os.Path =
    sys.env.get("XDG_STATE_HOME") match
      case Some(path) => os.Path(path) / appName
      case None       => os.home / ".local" / "state" / appName

  /** Directory for runtime files. This is where the application can store
    * temporary files that should not be preserved between reboots, or special
    * files such as sockets and named pipes, or lock files.
    */
  val runtime: os.Path =
    sys.env.get("XDG_RUNTIME_DIR") match
      case Some(path) => os.Path(path) / appName
      case None       => os.root / "tmp" / appName
