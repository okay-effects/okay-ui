package okay.business.faces

import okay.business.model.*
import okay.faces.View

/** HELP next to what it explains: `Part.help` reaches a face as a group under the mark `help` (a browser draws it as a
 * closed "?" opened in place) */
class HelpSuite extends munit.FunSuite:
  test("Part.help is a group under the mark `help`, titled by its topic, its parts inside") {
    val screen = Screen[Unit]("Result")(Part.section("What the plan did")(
      Part.help("What is a drawdown")(Part.text("The fall from a top.")), Part.details("Fall" -> "29.9%")))(
      Button("Back")(()))
    def find(v: View): List[View] = v match
      case m @ View.Marked("help", _) => List(m)
      case View.Marked(_, x) => find(x)
      case View.Group(_, cs) => cs.toList.flatMap(find)
      case _ => Nil
    find(OnFaces.view(screen, Nil)) match
      case List(View.Marked("help", View.Group(Some(t), cs))) =>
        assertEquals(t, "What is a drawdown")
        assertEquals(cs.toList, List(View.Text("The fall from a top.")))
      case other => fail(s"no help group: $other")
  }

  test("Part.path is the mark `path` around its text: the face's address of the screen") {
    val screen = Screen[Unit]("Planner")(Part.path("planner"), Part.text("x"))(Button("Back")(()))
    def find(v: View): Option[View] = v match
      case m @ View.Marked("path", _) => Some(m)
      case View.Marked(_, x) => find(x)
      case View.Group(_, cs) => cs.iterator.map(find).collectFirst { case Some(m) => m }
      case _ => None
    assertEquals(find(OnFaces.view(screen, Nil)),
      Some(View.Marked("path", View.Group(None, Vector(View.Text("planner"))))))
  }
