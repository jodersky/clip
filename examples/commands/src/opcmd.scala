package app.op

case class Op(factor: Double)

@clip.group(help = "Operations using a factor")
def op(factor: Double = 1.0) = Op(factor)

@clip.command(help = "Show the current factor")
def showFactor(
  @clip.context op: Op
) = clip.echo(s"the factor is ${op.factor}")

@clip.command(help = "Multiply the factor by an operand")
def multiply(operand: Double, @clip.context op: Op) = clip.echo(s"result is: ${op.factor * operand}")

val cmds = clip.commandFor(this)
