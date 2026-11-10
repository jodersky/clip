import utest.*
import clip.util.userdirs
import userdirs.Platform

object UserdirsTest extends TestSuite:

  val home = os.home

  val tests = Tests:

    test("linux defaults"):
      val d = userdirs("app", Map.empty, Platform.Linux)
      assert(d.config == home / ".config" / "app")
      assert(d.cache == home / ".cache" / "app")
      assert(
        d.data == Seq(
          home / ".local" / "share" / "app",
          os.root / "usr" / "local" / "share" / "app",
          os.root / "usr" / "share" / "app"
        )
      )
      assert(d.state == home / ".local" / "state" / "app")
      assert(d.runtime == os.root / "tmp" / "app")

    test("linux xdg"):
      val env = Map(
        "XDG_CONFIG_HOME" -> "/c",
        "XDG_CACHE_HOME" -> "/k",
        "XDG_DATA_HOME" -> "/d",
        "XDG_DATA_DIRS" -> "/d1::relative:/d2",
        "XDG_STATE_HOME" -> "/s",
        "XDG_RUNTIME_DIR" -> "/r"
      )
      val d = userdirs("app", env, Platform.Linux)
      assert(d.config == os.root / "c" / "app")
      assert(d.cache == os.root / "k" / "app")
      assert(
        d.data == Seq(
          os.root / "d" / "app",
          os.root / "d1" / "app",
          os.root / "d2" / "app"
        )
      )
      assert(d.state == os.root / "s" / "app")
      assert(d.runtime == os.root / "r" / "app")

    test("linux ignores relative and empty"):
      val env = Map("XDG_CONFIG_HOME" -> "relative", "XDG_CACHE_HOME" -> "")
      val d = userdirs("app", env, Platform.Linux)
      assert(d.config == home / ".config" / "app")
      assert(d.cache == home / ".cache" / "app")

    test("macos defaults"):
      val d = userdirs("app", Map("TMPDIR" -> "/var/folders/xy/T/"), Platform.MacOS)
      val support = home / "Library" / "Application Support" / "app"
      assert(d.config == support)
      assert(d.cache == home / "Library" / "Caches" / "app")
      assert(
        d.data == Seq(
          support,
          os.root / "Library" / "Application Support" / "app"
        )
      )
      assert(d.state == support)
      assert(d.runtime == os.root / "var" / "folders" / "xy" / "T" / "app")

    test("macos xdg override"):
      val d = userdirs("app", Map("XDG_CONFIG_HOME" -> "/c"), Platform.MacOS)
      assert(d.config == os.root / "c" / "app")

    test("windows"):
      // use unix-style absolute paths so that this test runs on any host
      val env = Map(
        "APPDATA" -> "/roaming",
        "LOCALAPPDATA" -> "/local",
        "ProgramData" -> "/programdata",
        "TEMP" -> "/temp"
      )
      val d = userdirs("app", env, Platform.Windows)
      assert(d.config == os.root / "roaming" / "app")
      assert(d.cache == os.root / "local" / "app" / "cache")
      assert(
        d.data == Seq(
          os.root / "roaming" / "app",
          os.root / "programdata" / "app"
        )
      )
      assert(d.state == os.root / "local" / "app")
      assert(d.runtime == os.root / "temp" / "app")

    test("windows fallbacks"):
      val d = userdirs("app", Map("ProgramData" -> "/pd"), Platform.Windows)
      assert(d.config == home / "AppData" / "Roaming" / "app")
      assert(d.cache == home / "AppData" / "Local" / "app" / "cache")
      assert(d.state == home / "AppData" / "Local" / "app")
      assert(d.runtime == home / "AppData" / "Local" / "Temp" / "app")
