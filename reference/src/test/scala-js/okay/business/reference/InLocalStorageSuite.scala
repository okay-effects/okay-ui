package okay.business.reference

import org.scalajs.dom

/** THE SAME SCRIPT, THE LEDGER IN THE PAGE'S STORAGE (jsdom's): emptied before each run, the same records */
class InLocalStorageSuite extends ExpensesSuite("in local storage", () =>
  dom.window.localStorage.removeItem("expenses-test")
  val storage = Ledger.localStorage("expenses-test")
  (storage, () => storage.entries)):

  test("in local storage: something unreadable there is no expenses, and is written over by the next"):
    dom.window.localStorage.setItem("expenses-test", "{not json")
    val storage = Ledger.localStorage("expenses-test")
    assertEquals(storage.entries, Nil)
    okay.business.scala3.Play(okay.business.scala3.steps(storage(Today.fixed(today)(Expenses.main[Expenses.App]))),
      add("Amount" -> "1", "Category" -> "Fun")*)
    assertEquals(storage.entries.map(_.amount), List(BigDecimal("1.00")))
