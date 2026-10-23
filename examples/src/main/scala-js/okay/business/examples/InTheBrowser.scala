package okay.business.examples

import okay.business.faces.OnFaces
import okay.effects.cek.OnCek.self
import okay.faces.Look
import okay.faces.laminar.LaminarFace

/** THE SHOP IN THE BROWSER: `sbt -Dplatforms=js examplesJS/web`, then open `examples/.js/target/web/index.html` — the
 * same business program as at the terminal, its screens on okay-faces' Laminar face, all of it on okay-scala's
 * machine, in the page. The program does not change; the face is this line */
object InTheBrowser:
  def main(args: Array[String]): Unit =
    LaminarFace.mount(OnFaces.program(scala3.Shop.app(scala3.Shop.Memory())),
      LaminarFace.Settings(look = Some(Look.default)))
