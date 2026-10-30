package okay.business.faces

import okay.business.model.*
import okay.faces.View

/** A tree on a screen: nodes under marks a page opens and closes, their keys marks of their own */
class TreeSuite extends munit.FunSuite:
  test("Part.tree: nodes, their state, keys, labels and children as okay-faces' views"):
    val t = Part.tree("Term", Part.TreeNode("0", "apply", open = true, children = List(
      Part.TreeNode("1", "λ x", current = true, children = List(Part.TreeNode("2", "x"))), Part.TreeNode("3", "con 1"))))
    val shown = OnFaces.view(Screen[Unit]("T")(t)(Button("OK")(())), Nil).toString
    assert(shown.contains("Marked(tree-node-open,Marked(tree-key-0"), shown)
    assert(shown.contains("Marked(tree-node-current,Marked(tree-key-1"), shown)
    assert(shown.contains("Marked(tree-leaf,Text(x))"), shown)
    assert(shown.contains("Marked(tree-children"), shown)
    assert(Skin.calm().css.contains(".okay-mark-tree-children"))
