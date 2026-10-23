package okay.business.examples

import com.raquo.laminar.api.L.*
import okay.business.faces.OnFaces
import okay.effects.cek.OnCek
import okay.faces.laminar.LaminarFace
import org.scalajs.dom

/** THE SHOP IN THE PAGE, under jsdom: the business program on okay-scala's machine, okay-faces' Laminar face showing it
 * — clicked through as a person would: a menu, a form typed into and submitted, a field's problem, a table */
class ShopPageSuite extends munit.FunSuite:
  def mounted(el: HtmlElement): dom.html.Element =
    dom.document.body.innerHTML = ""
    val container = dom.document.createElement("div").asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(container)
    render(container, el)
    container
  def q(root: dom.Element, selector: String): dom.html.Element =
    val e = root.querySelector(selector)
    assert(e != null, s"no $selector in: ${root.innerHTML}")
    e.asInstanceOf[dom.html.Element]
  def press(root: dom.Element, action: String): Unit = q(root, s"button[data-action=$action]").click()
  def typeInto(root: dom.Element, id: String, value: String): Unit =
    q(root, s"#$id").asInstanceOf[dom.html.Input].value = value
  def submit(root: dom.Element, action: String): Unit =
    q(root, s"form[data-action=$action]").dispatchEvent(new dom.Event("submit",
      new dom.EventInit { bubbles = true; cancelable = true }))

  test("a customer added through the form, then shown in the table"):
    val memory = scala3.Shop.Memory()
    val face = new LaminarFace[OnCek.type]()(OnCek)
    val root = mounted(face.handle(OnFaces.program(scala3.Shop.app(memory))))
    assertEquals(q(root, "h1").textContent, "Shop")
    press(root, "customers")
    assert(root.innerHTML.contains("no customers yet"), root.innerHTML)
    press(root, "add")
    typeInto(root, "f-name", "Ann"); typeInto(root, "f-email", "ann"); submit(root, "save")
    assert(root.innerHTML.contains("an e-mail address"), root.innerHTML)
    assertEquals(memory.customers, Nil)
    // okay-faces' Laminar face draws the form again empty after a problem: the person types it again
    typeInto(root, "f-name", "Ann"); typeInto(root, "f-email", "ann@example.com"); submit(root, "save")
    assertEquals(memory.customers.map(_.name), List("Ann"))
    assert(root.innerHTML.contains("Ann added"), root.innerHTML)
    press(root, "ok")
    assert(root.querySelector("table") != null && root.innerHTML.contains("ann@example.com"), root.innerHTML)
