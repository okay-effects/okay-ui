package okay.business.reference

import okay.business.faces.OnFaces
import okay.business.scala3.steps
import okay.business.reference.Expenses.App
import okay.effects.cek.OnCek.self
import okay.faces.Look
import okay.faces.laminar.LaminarFace

/** EXPENSES IN THE BROWSER: `sbt -Dplatforms=jvm,js referenceJS/web`, then open reference/.js/target/web/index.html —
 * the records in the page's storage, the date from the browser's clock, the screens on okay-faces' Laminar face. The
 * whole application is the line below */
object InTheBrowser:
  val page: LaminarFace.Settings = LaminarFace.Settings(look = Some(Look.default))

  def main(args: Array[String]): Unit =
    LaminarFace.mount(OnFaces.program(steps(Ledger.localStorage("expenses")(Today.system(Expenses.main[App])))), page)
