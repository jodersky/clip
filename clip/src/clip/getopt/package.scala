package clip.getopt

import collection.mutable as m

case class Arg(
    param: String, // the name or alias used
    value: Option[String] // always defined for positional params  )
)

case class ParseResult(
    parsed: m.ListBuffer[(String, Arg)],
    remainder: Option[(String, Iterator[String])],
    decls: Seq[ParamDecl]
):
  def grouped: m.Map[String, m.ListBuffer[Arg]] =
    val grouped = m.Map.empty[String, m.ListBuffer[Arg]]
    for param <- decls do grouped += param.name -> m.ListBuffer.empty
    for (name, arg) <- parsed do grouped(name) += arg
    grouped

// extractor for named arguments
private val Named = "(--?[^=]+)(?:=(.*))?".r

// used for special-casing short parameters
private val ShortNamed = "-([^-].*)".r

def parse(params: Seq[ParamDecl], args: Iterator[String]): ParseResult =
  val named = m.Map.empty[String, ParamDecl]
  val positional = m.ArrayBuffer.empty[ParamDecl]
  for param <- params do
    for name <- param.allNames do
      if name.startsWith("-") then named += name -> param
      else positional += param

  val results = m.ListBuffer.empty[(String, Arg)]

  var unknown: String = null

  var pos = 0 // index of current positional argument

  var arg: String = null
  def readArg() =
    if args.hasNext then arg = args.next()
    else arg = null
  readArg()

  var onlyPositionals = false
  def addPositional() =
    if pos < positional.length then
      val param = positional(pos)
      results += param.name -> Arg(param.name, Some(arg))

      if param.endOfNamed then onlyPositionals = true
      if !param.repeats then pos += 1
      readArg()
    else
      unknown = arg
      arg = null // stop parsing

  while arg != null do
    if (onlyPositionals) {
      addPositional()
    } else {
      arg match {
        case "--" =>
          onlyPositionals = true
          readArg()
        case Named(name, embedded) if named.contains(name) =>
          readArg()
          val param = named(name)
          if (embedded != null) { // embedded argument, i.e. one that contains '='
            results += param.name -> Arg(name, Some(embedded))
          } else if (param.flag) { // flags never take an arg
            results += param.name -> Arg(name, None)
          } else if (arg == null || arg.matches(Named.regex)) { // non-flags may have an arg
            results += param.name -> Arg(name, None)
          } else {
            results += param.name -> Arg(name, Some(arg))
            readArg()
          }
          if (param.endOfNamed) onlyPositionals = true
        case ShortNamed(name) =>

          // deal with combined single-letter options (the previous case
          // already took care of any named args that are known, long and
          // short)
          val letters = name.iterator
          while (letters.hasNext) {
            val option = s"-${letters.next()}"
            if (named.contains(option)) {
              val param = named(option)
              if (param.flag) { // flags never take an arg
                results += param.name -> Arg(option, None)
              } else if (letters.hasNext) {
                results += param.name -> Arg(option, Some(letters.mkString))
              } else {
                results += param.name -> Arg(option, None)
              }
              if (param.endOfNamed) onlyPositionals = true
            } else {
              // In case of an unknown short letter, the argument is reported
              // unknown as in the regular named case. In other words, the
              // remaining letters are consumed and any embedded values are
              // omitted
              val Named(name, _) = s"$option${letters.mkString}": @unchecked
              unknown = name
              arg = null // stop parsing
            }
          }
          if arg != null then readArg()
        case Named(_, _) =>
          unknown = arg
          arg = null // stop parsing
        case positional =>
          addPositional()
      }
    }
  end while

  ParseResult(
    results,
    if unknown == null then None
    else Some((unknown, args)),
    params
  )
end parse
