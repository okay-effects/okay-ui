package okay.business.dom

import okay.business.model.{Button, Events, Field, Form, Part, Screen, Style, Ui}
import okay.business.scala3.show
import okay.scala3.*
import okay.scala3.std.{Async, Lift, Lifted}
import org.scalajs.dom
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** A LIVE SCREEN IN A PAGE by the DOM face, under jsdom: shown again on every event, what was typed stays; a button at
 * its slot in the card; long text a text area. Into `Async`, driven by `Async.toFuture`; once the deprecated row of
 * `Lifted[Future]` */
class DomSuite extends munit.FunSuite:
  type Page[X] = (Async + Ui)[X]
  type Before[X] = (Lifted[Future] + Ui)[X]

  enum Got:
    case Line(n: Int)
    case Approved(n: Int)
    case Sent(text: String, n: Int)

  def timeline[R[_]](bus: Events.Manual[Int], n: Int)(using In[Ui, R]): String ! R =
    val note = Form.one(Field.longText("Note"))
    val screen = Screen[Got]("Timeline")(
      (1 to n).toList.map(i => Part.block("line")(Part.text(s"line $i"))) :+
        Part.block("card", "waiting")(Part.slot("w")) :+ note: _*)(
      Button("Approve", Style.Primary)(Got.Approved(n)).at("w"),
      Button.submit("Send", note)(t => Got.Sent(t, n))).until(bus.events.map(Got.Line(_)))
    show[Got, R](screen).flatMap {
      case Got.Line(i) => timeline[R](bus, i)
      case Got.Approved(at) => pure(s"approved at $at")
      case Got.Sent(t, at) => pure(s"$t at $at")
    }

  def settled: Future[Unit] =
    val p = Promise[Unit]()
    js.timers.setTimeout(0)(p.success(()))
    p.future
  def q(selector: String): dom.html.Element =
    val e = dom.document.querySelector(selector)
    assert(e != null, s"no $selector in: ${dom.document.body.innerHTML}")
    e.asInstanceOf[dom.html.Element]
  def fresh(): dom.Element =
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    root

  test("events: the timeline grows in place, the text typed stays, the first line is the node it was"):
    val root = fresh()
    val bus = Events.manual[Int]
    val result = Async.toFuture(Dom.run(root)(timeline[Page](bus, 1)))
    for
      _ <- settled
      area = q("textarea#f-note").asInstanceOf[dom.html.TextArea]
      first = q(".okay-mark-line p")
      _ = area.value = "half\ntyped"
      _ = bus.fire(2)
      _ <- settled
      _ = bus.fire(3)
      _ <- settled
      _ = assertEquals(dom.document.querySelectorAll(".okay-mark-line").length, 3)
      _ = assert(q(".okay-mark-line p") eq first, "the first line kept")
      _ = assert(q("textarea#f-note") eq area, "the text area kept")
      _ = assertEquals(area.value, "half\ntyped")
      _ = q("form[data-action=send]").dispatchEvent(new dom.Event("submit",
        new dom.EventInit { bubbles = true; cancelable = true }))
      done <- result
    yield assertEquals(done, "half\ntyped at 3")

  test("the look's stylesheet in the page, before the face"):
    val root = fresh()
    val bus = Events.manual[Int]
    val _ = Async.toFuture(Dom.run(root)(timeline[Page](bus, 1)))
    for _ <- settled
    yield assert(root.querySelector(".okay-ui > style") != null, root.innerHTML.take(300))

  test("a button at its slot: in the card, a ref; pressed after an event, it answers the screen shown now"):
    val root = fresh()
    val bus = Events.manual[Int]
    val result = Async.toFuture(Dom.run(root)(timeline[Page](bus, 1)))
    for
      _ <- settled
      _ = assert(q(".okay-mark-card a.okay-ref").textContent == "Approve", root.innerHTML)
      _ = assert(dom.document.querySelector("button[data-action=approve]") == null, "not in the bar")
      _ = bus.fire(4)
      _ <- settled
      _ = q(".okay-mark-card a.okay-ref").click()
      done <- result
    yield assertEquals(done, "approved at 4")

  test("deprecated, Lifted[Future]: the same live screen, lowered by Lift.lower"):
    val root = fresh()
    val bus = Events.manual[Int]
    val result = Lift.lower(Dom.run(root)(timeline[Before](bus, 1)))
    for
      _ <- settled
      _ = bus.fire(2)
      _ <- settled
      _ = q(".okay-mark-card a.okay-ref").click()
      done <- result
    yield assertEquals(done, "approved at 2")
