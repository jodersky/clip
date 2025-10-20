package clip.util.text

class WrappedLines(chars: Iterator[Char], width: Int) extends Iterator[String]:
  private val it = chars.buffered

  // unconsumed characters from previous calls to next()
  private val remainder = new StringBuilder()

  def hasNext: Boolean = it.hasNext || remainder.nonEmpty

  def next(): String =
    val line = new StringBuilder()
    var col = 0

    // first consume remainder
    while remainder.nonEmpty && col < width do
      val c = remainder.head
      line += c
      remainder.deleteCharAt(0)
      col += 1

    if col == width then
      // still more in remainder, leave it for next line
      return line.result()

    while it.hasNext && it.head.isWhitespace && col < width do
      val c = it.next()
      line += c
      col += 1

    // first word in line is special, it can be split on character if needed
    // rather than whitespace (this way we always print something)
    while it.hasNext && col < width && !it.head.isWhitespace do
      val c = it.next()
      line += c
      col += 1

    // if there's still more in the word, leave it for next line
    if col == width then return line.result()

    // subsequent words are only split on whitespace
    while it.hasNext && col < width do
      // consume whitespace
      while it.hasNext && it.head.isWhitespace && col < width do
        val c = it.next()
        line += c
        col += 1

      // consume word
      val wordBuf = new StringBuilder()
      while it.hasNext && !it.head.isWhitespace do
        val c = it.next()
        wordBuf += c

      val wordLen = wordBuf.length
      if col + wordLen > width then
        // word doesn't fit, leave it for next line
        remainder ++= wordBuf
        return line.result()
      else
        line ++= wordBuf
        col += wordLen

    line.result()

/** Print two columns of text with specified widths and an optional separator
  *
  * The input text is wrapped so that it fits within the specified column
  * widths. The separator is printed between the two columns additionally,
  * meaning that the total width used is `col1Width + separator.length +
  * col2Width`.
  */
def twoCols(
    col1: Iterable[Char],
    col2: Iterable[Char],
    col1Width: Int,
    col2Width: Int,
    separator: String = "",
    out: java.io.PrintStream
): Unit =
  val lines1 = WrappedLines(col1.iterator, col1Width)
  val lines2 = WrappedLines(col2.iterator, col2Width)

  while lines1.hasNext || lines2.hasNext do
    val left = if lines1.hasNext then lines1.next() else ""
    val right = if lines2.hasNext then lines2.next() else ""
    out.print(left)
    if right.nonEmpty then
      out.print(" " * (col1Width - left.length))
      out.print(separator)
    out.println(right)

def ellipsis(in: String, maxLength: Int): String =
  if in.length <= maxLength || maxLength < 3 then in
  else in.substring(0, maxLength - 3) + "..."

/** `thisIsKebabCase => this-is-kebab-case` */
def kebabify(camelCase: String): String = {
  val kebab = new StringBuilder
  var prevIsLower = false
  for (c <- camelCase) {
    if (prevIsLower && c.isUpper) {
      kebab += '-'
    }
    kebab += c.toLower
    prevIsLower = c.isLower
  }
  kebab.result()
}

// https://rosettacode.org/wiki/Jaro_similarity#C
def jaroSimilarity(s1: CharSequence, s2: CharSequence): Double =
  val l1 = s1.length()
  val l2 = s2.length()

  // if both strings are empty return 1
  // if only one of the strings is empty return 0
  if l1 == 0 then
    if l2 == 0 then return 1.0
    else return 0.0

  // max distance between two chars to be considered matching
  // floor() is ommitted due to integer division rules
  val maxDistance = math.max(l1, l2) / 2 - 1

  // arrays of bools that signify if that char in the matching string has a match
  val str1Matches = new Array[Boolean](l1)
  val str2Matches = new Array[Boolean](l2)

  // number of matches and transpositions
  var matches = 0.0;
  var transpositions = 0.0;

  // find the matches
  for i <- 0 until l1 do
    // start and end take into account the match distance
    val start = math.max(0, i - maxDistance)
    val end = math.min(i + maxDistance + 1, l2)

    var k = start
    var done = false
    while k < end && !done do
      // if str2 already has a match continue
      // if str1 and str2 are not
      if str2Matches(k) || s1.charAt(i) != s2.charAt(k) then ()
      else
        str1Matches(i) = true
        str2Matches(k) = true
        matches += 1
        done = true
      k += 1
    end while
  end for

  // if there are no matches return 0
  if matches == 0 then return 0.0

  // count transpositions
  var k = 0
  for i <- 0 until l1 do
    // if there are no matches in str1 continue
    if !str1Matches(i) then ()
    else
      // while there is no match in str2 increment k
      while !str2Matches(k) do k += 1
      // increment transpositions
      if s1.charAt(i) != s2.charAt(k) then transpositions += 1
      k += 1
  end for

  // divide the number of transpositions by two as per the algorithm specs
  // this division is valid because the counted transpositions include both
  // instances of the transposed characters.
  transpositions /= 2.0;

  // return the Jaro distance
  (matches / l1 + matches / l2 + (matches - transpositions) / matches) / 3.0

def jaroWinklerSimilarity(s1: CharSequence, s2: CharSequence): Double =
  var prefix = 0
  while prefix <= 4 && // maximum of four letters
    prefix < s1.length() &&
    prefix < s2.length() &&
    s1.charAt(prefix) == s2.charAt(prefix)
  do prefix += 1

  val jd = jaroSimilarity(s1, s2)
  jd + prefix * 0.1 * (1 - jd)

def jaroWinklerClosest(
    str: String,
    options: Iterable[String],
    minSimilarity: Double = 0.7
): Iterable[String] =
  options
    .map(s => s -> jaroWinklerSimilarity(str, s))
    .filter(_._2 >= minSimilarity)
    .toSeq
    .sortBy(-_._2)
    .map(_._1)
