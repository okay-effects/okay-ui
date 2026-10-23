package okay.business.laminar

import okay.business.faces.Skin
import okay.effects.cek.OnCek
import okay.effects.tagless.Program
import okay.faces.Face
import okay.faces.laminar.LaminarFace
import org.scalajs.dom

/**
 * A PAGE'S LOOK, built in (specs/business.md, "How a page looks"): `install(skin)` puts the skin's stylesheet into
 * the document's head — once; installed again it replaces the one before — and `mount(program)` is okay-faces'
 * Laminar face in the page, dressed in `Skin.default` unless another is said.
 */
object Page:
  /** the id of the `<style>` a skin is installed as: one per document */
  val styleId = "okay-ui-skin"

  def install(skin: Skin = Skin.default): Unit =
    val doc = dom.document
    val style = Option(doc.getElementById(styleId)).getOrElse {
      val made = doc.createElement("style")
      made.id = styleId
      doc.head.appendChild(made)
      made
    }
    style.textContent = skin.css

  /** the face's settings that wear `skin`: its look (okay-faces' tokens, rules and words) */
  def settings(skin: Skin, browserHistory: Boolean = false): LaminarFace.Settings =
    LaminarFace.Settings(look = Some(skin.look), browserHistory = browserHistory)

  /** THE HANDLER, whole, in the page: `Page.mount(OnFaces.program(steps(app)))` */
  def mount[A](p: Program[A, Face], skin: Skin = Skin.default, browserHistory: Boolean = false): Unit =
    install(skin)
    LaminarFace.mount[OnCek.type, A](p, settings(skin, browserHistory))(OnCek)
