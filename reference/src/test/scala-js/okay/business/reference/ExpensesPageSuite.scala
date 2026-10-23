package okay.business.reference

import com.raquo.laminar.api.L.*
import okay.business.faces.OnFaces
import okay.business.scala3.steps
import okay.business.reference.Expenses.App
import okay.effects.cek.OnCek
import okay.faces.laminar.LaminarFace
import org.scalajs.dom

/** THE BROWSER'S LINE IN A PAGE, under jsdom: the ledger in the page's storage, the date from its clock, okay-faces'
 * Laminar face — an expense added as a person would, then the page mounted again, as a reload: it is still there */
class ExpensesPageSuite extends munit.FunSuite:
  /** the line of `InTheBrowser`, drawn into a new page body */
  def page(): dom.html.Element =
    dom.document.body.innerHTML = ""
    val container = dom.document.createElement("div").asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(container)
    val face = new LaminarFace[OnCek.type]()(OnCek)
    render(container,
      face.handle(OnFaces.program(steps(Ledger.localStorage("expenses")(Today.system(Expenses.main[App]))))))
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

  test("an expense added on the page is in the page's storage, and there after a reload"):
    dom.window.localStorage.removeItem("expenses")
    val root = page()
    assertEquals(q(root, "h1").textContent, "Expenses")
    press(root, "add-an-expense")
    // the day starts at today, the browser's
    val now = new scala.scalajs.js.Date()
    assertEquals(q(root, "#f-day").asInstanceOf[dom.html.Input].value,
      okay.business.scala3.Day(now.getFullYear().toInt, now.getMonth().toInt + 1, now.getDate().toInt).toString)
    typeInto(root, "f-amount", "0"); submit(root, "save")
    assert(root.innerHTML.contains("more than zero"), root.innerHTML)
    typeInto(root, "f-amount", "12.5")
    q(root, "#f-category").asInstanceOf[dom.html.Select].value = "Transport"
    typeInto(root, "f-note", "bus"); submit(root, "save")
    assert(root.innerHTML.contains("12.50 EUR for Transport"), root.innerHTML)
    assertEquals(Ledger.localStorage("expenses").entries.map(e => (e.amount, e.category, e.note)),
      List((BigDecimal("12.50"), "Transport", Some("bus"))))
    val reloaded = page()
    press(reloaded, "all-expenses")
    assert(reloaded.innerHTML.contains("12.50 EUR") && reloaded.innerHTML.contains("bus"), reloaded.innerHTML)
