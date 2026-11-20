package okay.business.web

import okay.business.scala3.*
import org.scalajs.dom

/** Settings in the page's local storage, under jsdom: kept under the prefix, read back by another run */
class InThePageSuite extends munit.FunSuite:
  test("a setting kept in one run is read by the next, under its prefix"):
    type App[X] = Settings[X]
    run(InThePage.kept("app.")(keep[App]("key", "abc")))
    assertEquals(dom.window.localStorage.getItem("app.key"), "abc")
    assertEquals(run(InThePage.kept("app.")(setting[App]("key"))), Some("abc"))
    assertEquals(run(InThePage.kept("other.")(setting[App]("key"))), None)

  test("an address in the URL's fragment: each place a new entry of the history; opened where it says"):
    type App[X] = Address[X]
    val before = dom.window.history.length
    run(InThePage.address(nowAt[App](List("uk", "pesel", "who=ua")).andThen(nowAt[App](List("uk", "pesel", "who=ua")))))
    assertEquals(dom.window.location.hash, "#/uk/pesel/who=ua")
    assertEquals(dom.window.history.length, before + 1) // the same place said twice is one entry
    assertEquals(run(InThePage.address(whereOpened[App])), List("uk", "pesel", "who=ua"))
    run(InThePage.address(nowAt[App](List("ru", "дом"))))
    assertEquals(run(InThePage.address(whereOpened[App])), List("ru", "дом"))
    run(InThePage.address(nowAt[App](Nil)))
    assertEquals(dom.window.location.hash, "")
