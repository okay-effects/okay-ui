package okay.business.laminar

import okay.business.model.{Button, Field, Form, Part, Screen, Ui}
import okay.business.scala3.{show, working}
import okay.scala3.*
import org.scalajs.dom
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** `Ui` IN A PAGE, under jsdom: a form answered by the person, then work the world answers later — a promise of the
 * test's, as a `fetch` would be — shown as a working screen while it waits, then the answer on a screen of its own */
class LaminarSuite extends munit.FunSuite:
  type Page[X] = (Lifted[Future] + Ui)[X]

  def program(later: Future[Int]): String ! Page =
    val name = Form.one(Field.text("Name"))
    for
      who <- show[String, Page](Screen[String]("Who?")(name)(Button.submit("Go", name)(identity)))
      _ <- working[Page]("Working")(Part.text(s"asking for $who"), Part.link("the source", "https://example.org/"))
      n <- Lift.lift[Future, Int, Page](later)
      done <- show[String, Page](Screen[String]("Answer")(Part.text(s"$who: $n"))(Button("OK")("over")))
    yield done

  /** the browser's turn: the microtasks run, the page drawn again */
  def settled: Future[Unit] =
    val p = Promise[Unit]()
    js.timers.setTimeout(0)(p.success(()))
    p.future

  def q(selector: String): dom.html.Element =
    val e = dom.document.querySelector(selector)
    assert(e != null, s"no $selector in: ${dom.document.body.innerHTML}")
    e.asInstanceOf[dom.html.Element]

  test("a screen answered by a press; a working screen while the world answers; the next screen in its place"):
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    val later = Promise[Int]()
    val result = Lift.lower(Laminar.run(root)(program(later.future)))
    for
      _ <- settled
      _ = assertEquals(q("h1").textContent, "Who?")
      _ = q("#f-name").asInstanceOf[dom.html.Input].value = "Ann"
      _ = q("form[data-action=go]").dispatchEvent(new dom.Event("submit",
        new dom.EventInit { bubbles = true; cancelable = true }))
      _ <- settled
      _ = assertEquals(q("h1").textContent, "Working")
      _ = assert(root.textContent.contains("asking for Ann"), root.innerHTML)
      _ = assertEquals(q("a.okay-link").getAttribute("href"), "https://example.org/")
      _ = later.success(42)
      _ <- settled
      _ = assertEquals(q("h1").textContent, "Answer")
      _ = assert(root.textContent.contains("Ann: 42"), root.innerHTML)
      _ = q("button[data-action=ok]").click()
      done <- result
    yield assertEquals(done, "over")
