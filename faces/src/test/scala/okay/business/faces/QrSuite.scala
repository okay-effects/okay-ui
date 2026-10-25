package okay.business.faces

import okay.business.model.*
import okay.faces.View

/** A QR CODE ON A SCREEN: the part okay-faces' picture of it, its words the alt or the text */
class QrSuite extends munit.FunSuite:
  test("Part.qr is okay-faces' QR picture; too long a text, the text") {
    val screen = Screen[Unit]("Take it")(Part.qr("https://example.org/#take=0123", "the page"))(Button("OK")(()))
    val shown = OnFaces.view(screen, Nil)
    def pictures(v: View): List[View.Picture] = v match
      case p: View.Picture => List(p)
      case View.Group(_, cs) => cs.toList.flatMap(pictures)
      case _ => Nil
    val ps = pictures(shown)
    assertEquals(ps.map(_.text), List("the page"))
    assert(ps.head.shapes.size > 20)
    assertEquals(OnFaces.view(Screen[Unit]("Long")(Part.qr("q" * 3000))(Button("OK")(())), Nil) match
      case View.Group(_, cs) => cs.exists(_ == View.Text("q" * 3000))
      case _ => false, true)
  }
