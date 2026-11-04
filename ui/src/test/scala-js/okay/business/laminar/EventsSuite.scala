package okay.business.laminar

import okay.business.model.{Button, Events, Part, Screen}
import okay.business.scala3.show
import okay.scala3.*
import okay.scala3.std.Async
import org.scalajs.dom
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** A SCREEN ANSWERED BY AN EVENT in a page run by `Laminar.run` into `Async` (specs/business.md, "Events"): an event
 * that comes later answers the screen as a button would, and stops being listened to; a press first stops it too;
 * the program goes on, its async work and its next screens as before */
class EventsSuite extends munit.FunSuite:
  type Paged[X] = (Async + okay.business.model.Ui)[X]

  def settled: Future[Unit] =
    val p = Promise[Unit]()
    js.timers.setTimeout(0)(p.success(()))
    p.future
  def fresh(): dom.Element =
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    root
  def text: String = dom.document.body.textContent

  /** events of the test's: `fire` gives one to whoever listens; `listening` how many do */
  final class Source[A]:
    var listeners = Vector.empty[A => Unit]
    def listening: Int = listeners.size
    def fire(a: A): Unit = listeners.foreach(_(a))
    val events: Events[A] = Events[A] { k =>
      listeners :+= k
      () => listeners = listeners.filterNot(_ eq k)
    }

  def program(src: Source[String], later: Future[Int]): String ! Paged =
    for
      got <- show[String, Paged](Screen[String]("Waiting")(Part.text("for an event"))(Button("Skip")("skipped"))
        .until(src.events))
      n <- Async.fromFuture[Int, Paged](later)
      done <- show[String, Paged](Screen[String]("Next")(Part.text(s"$got $n"))(Button("OK")(s"$got $n")))
    yield done

  test("an event later answers the screen, the listening stops, the program goes on with its async work") {
    val root = fresh()
    val src = Source[String]()
    val later = Promise[Int]()
    val result = Async.toFuture(Laminar.run(root)(program(src, later.future)))
    for
      _ <- settled
      _ = assert(text.contains("for an event"), text)
      _ = assertEquals(src.listening, 1)
      _ = src.fire("tick")
      _ <- settled
      _ = assertEquals(src.listening, 0, "listened to no more once answered")
      _ = later.success(7)
      _ <- settled
      _ = assert(text.contains("tick 7"), text)
      _ = dom.document.querySelector("[data-action=ok]").asInstanceOf[dom.html.Element].click()
      r <- result
    yield assertEquals(r, "tick 7")
  }

  test("a press first answers it; the events then not listened to") {
    val root = fresh()
    val src = Source[String]()
    val result = Async.toFuture(Laminar.run(root)(program(src, Future.successful(1))))
    for
      _ <- settled
      _ = dom.document.querySelector("[data-action=skip]").asInstanceOf[dom.html.Element].click()
      _ <- settled
      _ = assertEquals(src.listening, 0)
      _ = src.fire("late")
      _ <- settled
      _ = assert(text.contains("skipped 1"), text)
      _ = dom.document.querySelector("[data-action=ok]").asInstanceOf[dom.html.Element].click()
      r <- result
    yield assertEquals(r, "skipped 1")
  }
