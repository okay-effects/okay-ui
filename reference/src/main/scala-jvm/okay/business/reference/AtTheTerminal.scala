package okay.business.reference

import java.nio.file.Path
import okay.business.faces.OnFaces
import okay.business.scala3.steps
import okay.business.reference.Expenses.App
import okay.effects.cek.OnCek.self
import okay.faces.term.Terminal

/** EXPENSES AT A TERMINAL: `sbt reference/run [file]` — the records in a JSON file (expenses.json unless given), the
 * date from the system's clock, the screens on okay-faces' terminal face. The whole application is the line below */
object AtTheTerminal:
  def main(args: Array[String]): Unit =
    val path = Path.of(args.headOption.getOrElse("expenses.json"))
    okay.faces.run(Terminal.console(OnFaces.program(steps(Ledger.file(path)(Today.system(Expenses.main[App]))))))
