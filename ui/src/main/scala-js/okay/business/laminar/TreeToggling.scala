package okay.business.laminar

import org.scalajs.dom

/**
 * A TREE OPENED AND CLOSED BY A CLICK, in a page (`Part.tree`): a node's label toggles its node between the marks
 * `tree-node` and `tree-node-open`; what the person opened or closed is kept by the node's key while the page lives,
 * so a tree drawn again (the next screen) shows it as they left it. okay-faces' markup is untouched: only classes.
 */
object TreeToggling:
  /** the person's choices, by node key: open or closed */
  private val chosen = scala.collection.mutable.Map.empty[String, Boolean]

  private def key(node: dom.Element): Option[String] =
    Option(node.firstElementChild).toList.flatMap(_.classList.toArray).find(_.startsWith("okay-mark-tree-key-"))
      .map(_.stripPrefix("okay-mark-tree-key-"))

  private def isNode(cls: String) = cls.startsWith("okay-mark-tree-node")

  private def set(node: dom.Element, open: Boolean): Unit =
    val now = node.classList.toArray.find(isNode).getOrElse("okay-mark-tree-node")
    val current = now.endsWith("-current")
    val next = "okay-mark-tree-node" + (if open then "-open" else "") + (if current then "-current" else "")
    if now != next then
      node.classList.remove(now)
      node.classList.add(next)

  private def opened(node: dom.Element): Boolean =
    node.classList.toArray.exists(c => c.startsWith("okay-mark-tree-node-open"))

  /** after a screen is drawn into `root`: each tree's nodes as the person left them, their labels clickable */
  def attach(root: dom.Element): Unit =
    val nodes = root.querySelectorAll("[class*='okay-mark-tree-node']")
    for i <- 0 until nodes.length do
      val node = nodes(i)
      key(node).foreach(k => chosen.get(k).foreach(open => set(node, open)))
      val label = node.querySelector(":scope > .okay-mark > .okay-group > .okay-mark-tree-label")
      if label != null && !label.hasAttribute("data-okay-toggle") then
        label.setAttribute("data-okay-toggle", "")
        label.addEventListener("click", (_: dom.Event) =>
          val open = !opened(node)
          set(node, open)
          key(node).foreach(chosen(_) = open))

  def attachSoon(root: dom.Element): Unit =
    attach(root)
    scalajs.js.timers.setTimeout(0)(attach(root))

  /** forget what was opened: a test's fresh start */
  def forget(): Unit = chosen.clear()

extension (cl: dom.DOMTokenList)
  private[laminar] def toArray: Array[String] = (0 until cl.length).map(cl(_)).toArray
