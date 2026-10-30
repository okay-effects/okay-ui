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
