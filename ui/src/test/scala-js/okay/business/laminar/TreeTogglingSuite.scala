package okay.business.laminar

import okay.business.model.{Button, Part, Screen, Ui}
import okay.business.scala3.show
import okay.scala3.*
import okay.scala3.std.Async
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** A tree in a page, under jsdom: a click opens and closes a node; drawn again, it is as the person left it */
class TreeTogglingSuite extends munit.FunSuite:
  def screen = Screen[Unit]("Tree")(Part.tree("Term", Part.TreeNode("a", "apply", children = List(Part.TreeNode("b", "x")))))(
    Button("OK")(()))

  def draw(): (dom.Element, okay.scala3.std.Running[Unit]) =
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    type Page[X] = (Async + Ui)[X]
    val running = Async.start(Laminar.run(root)(show[Unit, Page](screen)))
    TreeToggling.attach(root)
    (root, running)

  def cls(e: dom.Element): String = e.getAttribute("class") + " "
  def node(root: dom.Element) = root.querySelector("[class*='okay-mark-tree-node']")

  test("a click on a label opens its node, again closes it; the next drawing keeps it open"):
    TreeToggling.forget()
    val (root, first) = draw()
    assert(cls(node(root)).contains("okay-mark-tree-node "), cls(node(root)))
    root.querySelector(".okay-mark-tree-label").asInstanceOf[dom.HTMLElement].click()
    assert(cls(node(root)).contains("okay-mark-tree-node-open"), cls(node(root)))
    first.cancel()
    val (again, second) = draw()
    assert(cls(node(again)).contains("okay-mark-tree-node-open"), cls(node(again)))
    again.querySelector(".okay-mark-tree-label").asInstanceOf[dom.HTMLElement].click()
    assert(!cls(node(again)).contains("okay-mark-tree-node-open"), cls(node(again)))
    second.cancel()
