package clip.util

/** Utility class for getting *user* directories for common types of application
  * data.
  *
  * Use this only if your command line app is run in a *user* context, NOT a
  * *system* context (e.g., a daemon).
  *
  * The directories are chosen according to the conventions of the current
  * platform:
  *
  *   - Linux and other Unix-like systems: the XDG Base Directory Specification.
  *   - macOS: `~/Library/Application Support` and `~/Library/Caches`.
  *   - Windows: the `%APPDATA%` (roaming) and `%LOCALAPPDATA%` (local) known
  *     folders.
  *
  * On all platforms, explicitly set XDG environment variables (e.g.
  * `XDG_CONFIG_HOME`) take precedence over the platform defaults. This allows
  * users who prefer XDG-style layouts on macOS or Windows to opt into them.
  *
  * @param appName
  *   the name of the application; used as the final path segment of every
  *   directory
  * @param env
  *   the environment variables to consult (defaults to the process
  *   environment)
  * @param platform
  *   the platform conventions to use (defaults to the current platform)
  */
class userdirs(
    appName: String,
    env: collection.Map[String, String] = sys.env,
    platform: userdirs.Platform = userdirs.Platform.current
):
  import userdirs.Platform

  /** Read an environment variable as an absolute path. Unset, empty and
    * relative values are ignored, as mandated by the XDG specification.
    */
  private def envPath(name: String): Option[os.Path] =
    env
      .get(name)
      .filter(p => p.nonEmpty && java.nio.file.Paths.get(p).isAbsolute)
      .map(os.Path(_))

  private def envPaths(name: String): Option[Seq[os.Path]] =
    env.get(name).filter(_.nonEmpty).map { paths =>
      paths
        .split(java.io.File.pathSeparator)
        .toSeq
        .filter(p => p.nonEmpty && java.nio.file.Paths.get(p).isAbsolute)
        .map(os.Path(_))
    }

  // macOS
  private def macAppSupport = os.home / "Library" / "Application Support"
  private def macCaches = os.home / "Library" / "Caches"

  // Windows
  private def winRoaming =
    envPath("APPDATA").getOrElse(os.home / "AppData" / "Roaming")
  private def winLocal =
    envPath("LOCALAPPDATA").getOrElse(os.home / "AppData" / "Local")
  private def winProgramData =
    envPath("ProgramData")
      .orElse(envPath("PROGRAMDATA"))
      .orElse(envPath("ALLUSERSPROFILE"))
      .getOrElse(os.Path("C:\\ProgramData"))

  /** Directory containing configuration files. These files are typically
    * human-editable, and should generally be treated as read-only by the
    * application (except if the application is specifically manipulating its
    * own configuration).
    *
    * You can think of it as the equivalent of `/etc/<appName>` in a system-wide
    * installation, but for user-specific configuration files.
    *
    * Defaults:
    *   - Linux: `$XDG_CONFIG_HOME/<appName>` or `~/.config/<appName>`
    *   - macOS: `~/Library/Application Support/<appName>`
    *   - Windows: `%APPDATA%\<appName>`
    */
  val config: os.Path =
    val base = envPath("XDG_CONFIG_HOME").getOrElse:
      platform match
        case Platform.Linux   => os.home / ".config"
        case Platform.MacOS   => macAppSupport
        case Platform.Windows => winRoaming
    base / appName

  /** Directory for cached data files. These files can be regenerated or
    * downloaded again if deleted.
    *
    * You can think of it as the equivalent of `/var/cache/<appName>` in a
    * system-wide installation, but for user-specific cached data files.
    *
    * Defaults:
    *   - Linux: `$XDG_CACHE_HOME/<appName>` or `~/.cache/<appName>`
    *   - macOS: `~/Library/Caches/<appName>`
    *   - Windows: `%LOCALAPPDATA%\<appName>\cache`
    */
  val cache: os.Path =
    envPath("XDG_CACHE_HOME") match
      case Some(base) => base / appName
      case None =>
        platform match
          case Platform.Linux   => os.home / ".cache" / appName
          case Platform.MacOS   => macCaches / appName
          case Platform.Windows => winLocal / appName / "cache"

  /** Preference-ordered directories for data files. This always contains at
    * least one element, the first one being the user-specific directory.
    *
    * Data files are files that are necessary for the application to function,
    * but don't contain the application's state (e.g., static images, templates,
    * etc.).
    *
    * You can think of it as the equivalent of `/usr/share/<appName>` in a
    * system-wide installation, but for user-specific data files.
    *
    * Defaults:
    *   - Linux: `$XDG_DATA_HOME/<appName>` or `~/.local/share/<appName>`,
    *     followed by `$XDG_DATA_DIRS` or `/usr/local/share/<appName>` and
    *     `/usr/share/<appName>`
    *   - macOS: `~/Library/Application Support/<appName>`, followed by
    *     `/Library/Application Support/<appName>`
    *   - Windows: `%APPDATA%\<appName>`, followed by `%ProgramData%\<appName>`
    */
  val data: Seq[os.Path] =
    val home = envPath("XDG_DATA_HOME").getOrElse:
      platform match
        case Platform.Linux   => os.home / ".local" / "share"
        case Platform.MacOS   => macAppSupport
        case Platform.Windows => winRoaming
    val other = envPaths("XDG_DATA_DIRS").getOrElse:
      platform match
        case Platform.Linux =>
          Seq(os.root / "usr" / "local" / "share", os.root / "usr" / "share")
        case Platform.MacOS =>
          Seq(os.root / "Library" / "Application Support")
        case Platform.Windows =>
          Seq(winProgramData)
    (home +: other).map(_ / appName).distinct

  /** Directory for state files. This is where the application can store its
    * state that should persist between runs, such as database files.
    *
    * You can think of it as the equivalent of `/var/lib/<appName>` in a
    * system-wide installation, but for user-specific state files.
    *
    * Defaults:
    *   - Linux: `$XDG_STATE_HOME/<appName>` or `~/.local/state/<appName>`
    *   - macOS: `~/Library/Application Support/<appName>`
    *   - Windows: `%LOCALAPPDATA%\<appName>`
    */
  val state: os.Path =
    val base = envPath("XDG_STATE_HOME").getOrElse:
      platform match
        case Platform.Linux   => os.home / ".local" / "state"
        case Platform.MacOS   => macAppSupport
        case Platform.Windows => winLocal
    base / appName

  /** Directory for runtime files. This is where the application can store
    * temporary files that should not be preserved between reboots, or special
    * files such as sockets and named pipes, or lock files.
    *
    * Defaults:
    *   - Linux: `$XDG_RUNTIME_DIR/<appName>` or `/tmp/<appName>`
    *   - macOS: `$TMPDIR/<appName>` (a per-user directory) or `/tmp/<appName>`
    *   - Windows: `%TEMP%\<appName>` or `%LOCALAPPDATA%\Temp\<appName>`
    */
  val runtime: os.Path =
    val base = envPath("XDG_RUNTIME_DIR").getOrElse:
      platform match
        case Platform.Linux => os.root / "tmp"
        case Platform.MacOS => envPath("TMPDIR").getOrElse(os.root / "tmp")
        case Platform.Windows =>
          envPath("TEMP")
            .orElse(envPath("TMP"))
            .getOrElse(winLocal / "Temp")
    base / appName

object userdirs:

  /** The platform conventions used to determine directories. */
  enum Platform:
    /** Linux and other Unix-like systems (XDG Base Directory Specification) */
    case Linux
    case MacOS
    case Windows

  object Platform:
    lazy val current: Platform =
      val name = System.getProperty("os.name", "").toLowerCase
      if name.startsWith("windows") then Platform.Windows
      else if name.startsWith("mac") || name.startsWith("darwin") then
        Platform.MacOS
      else Platform.Linux
