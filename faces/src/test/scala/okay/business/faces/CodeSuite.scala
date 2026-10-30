package okay.business.faces

import okay.business.model.*
import okay.faces.View

/** TEXT AS WRITTEN ON A SCREEN: its lines kept, the current ones marked, under the mark the skin dresses */
class CodeSuite extends munit.FunSuite:
  test("Part.code is a group of marked lines under the mark `code`, spaces kept") {
    val screen = Screen[Unit]("Step")(Part.code("(lam x\n  [x x])\nerror", 1))(Button("OK")(()))
    def code(v: View): List[View] = v match
      case m @ View.Marked("code", _) => List(m)
      case View.Group(_, cs) => cs.toList.flatMap(code)
      case View.Marked(_, x) => code(x)
      case _ => Nil
    code(OnFaces.view(screen, Nil)) match
      case List(View.Marked(_, View.Group(None, lines))) =>
        assertEquals(lines.toList, List(View.Marked("code-line", View.Text("(lam x")),
          View.Marked("code-line-current", View.Text("  [x x])")), View.Marked("code-line", View.Text("error"))))
      case other => fail(s"not a code block: $other")
  }

  test("a titled code block keeps its title; the skin dresses the marks") {
    val part = Part.code("Term", List("a", "b"), Set(0))
    assertEquals(part, Part.Code(List("a", "b"), Set(0), Some("Term")))
    val css = Skin.calm().css
    assert(css.contains(".okay-mark-code-line-current"))
    assert(css.contains("white-space: pre"))
  }
