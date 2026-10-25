package okay.business.laminar

import okay.business.faces.Skin
import okay.business.model.{Button, Screen, Ui}
import okay.business.scala3.show
import okay.scala3.*
import okay.scala3.std.{Lift, Lifted}
import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** A PAGE'S LOOK, under jsdom: the default skin with nothing said, one `<style>` however often a skin is installed */
class PageSuite extends munit.FunSuite:
  def skins: List[dom.Element] =
    (0 until dom.document.head.children.length).map(dom.document.head.children(_))
      .filter(_.id == Page.styleId).toList

  test("a skin installed again replaces the one before"):
    Page.install(Skin.calm(accent = "#0b7a5c"))
    Page.install(Skin.plain)
    Page.install()
    assertEquals(skins.map(_.textContent), List(Skin.default.css))

  test("a page run with nothing said wears the default skin"):
    Page.install(Skin.plain)
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    type Page_[X] = (Lifted[Future] + Ui)[X]
    Lift.lower(Laminar.run(root)(show[Unit, Page_](Screen[Unit]("Hello")()(Button("OK")(())))))
    assertEquals(skins.map(_.textContent), List(Skin.default.css))
