package clip.completion

enum Completer:
  /** No completion */
  case Empty

  /** Completion picked from a fixed set of words */
  case Fixed(alternatives: Set[String])

  /** Default bash completion (uses paths) */
  case Default

  /** Invoke a function to produce completions
    * @param complete
    *   Function that takes the current (partial) input and returns a sequence
    *   of possible completions.
    */
  case Dynamic[A](complete: (A, String) => Set[String])
