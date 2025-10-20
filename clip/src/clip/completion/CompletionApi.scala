package clip.completion

trait CompletionApi:

  trait CompleterFor[A]:
    def completer: Completer

  given fallbackCompleterFor[A]: CompleterFor[A] with
    def completer: Completer = Completer.Empty

  given absPathCompleter: CompleterFor[os.Path] with
    def completer: Completer = Completer.Default

  given relPathCompleter: CompleterFor[os.RelPath] with
    def completer: Completer = Completer.Default

  given subPathCompleter: CompleterFor[os.SubPath] with
    def completer: Completer = Completer.Default

  given jPathCompleter: CompleterFor[java.nio.file.Path] with
    def completer: Completer = Completer.Default

  given jFileCompleter: CompleterFor[java.io.File] with
    def completer: Completer = Completer.Default
