package okay.business.reference

import okay.business.faces.{OnFaces, Skin}
import okay.business.laminar.Page
import okay.business.scala3.steps
import okay.business.reference.Expenses.App

/** EXPENSES IN THE BROWSER: `sbt -Dplatforms=jvm,js referenceJS/web`, then open reference/.js/target/web/index.html —
 * the records in the page's storage, the date from the browser's clock, the screens on okay-faces' Laminar face. The
 * whole application is the line below, in okay-ui's look with the application's green */
object InTheBrowser:
  val skin: Skin = Skin.calm(accent = "#0b7a5c", deep = "#0d3b30")

  def main(args: Array[String]): Unit =
    Page.mount(OnFaces.program(steps(Ledger.localStorage("expenses")(Today.system(Expenses.main[App])))), skin)
