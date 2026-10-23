package okay.business.examples

import okay.business.faces.OnFaces
import okay.effects.cek.OnCek.self
import okay.faces.term.Terminal

/** THE SHOP AT A TERMINAL: `sbt examples/run` — the business program on okay-scala, its screens on okay-faces'
 * terminal face, run on okay-scala's machine too (`OnCek`). Another face is another line here; the program does not
 * change */
object AtTheTerminal:
  def main(args: Array[String]): Unit =
    okay.faces.run(Terminal.console(OnFaces.program(scala3.Shop.app(scala3.Shop.Memory()))))
