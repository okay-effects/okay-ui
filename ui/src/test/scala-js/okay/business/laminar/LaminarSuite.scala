package okay.business.laminar

import okay.business.model.{Button, Field, Form, Part, Screen, Ui}
import okay.business.scala3.{interruptible, show, working}
import okay.scala3.*
import okay.scala3.std.{Async, Cancelled, Lift, Lifted}
import org.scalajs.dom
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** `Ui` IN A PAGE, under jsdom: a form answered by the person, then work the world answers later — a promise of the
 * test's, as a `fetch` would be — shown as a working screen while it waits, then the answer on a screen of its own.
 * Into `Async`, driven by `Async.toFuture`, cancelled by `Async.start`'s `Running`; and the deprecated row of
 * `Lifted[Future]`, lowered by `Lift.lower` */
class LaminarSuite extends munit.FunSuite:
  type Page[X] = (Lifted[Future] + Ui)[X]
  type Paged[X] = (Async + Ui)[X]

  def asynchronous(later: Future[Int]): String ! Paged =
    val name = Form.one(Field.text("Name"))
    for
      who <- show[String, Paged](Screen[String]("Who?")(name)(Button.submit("Go", name)(identity)))
      _ <- working[Paged]("Working")(Part.text(s"asking for $who"), Part.link("the source", "https://example.org/"))
      n <- Async.fromFuture[Int, Paged](later)
      done <- show[String, Paged](Screen[String]("Answer")(Part.text(s"$who: $n"))(Button("OK")("over")))
    yield done

  /** DEPRECATED, the way before: the world's answer a `Lifted[Future]` */
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

  def fresh(): dom.Element =
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    root

  test("Async: a screen answered by a press; a working screen while the world answers; the next in its place"):
    val root = fresh()
    val later = Promise[Int]()
    val result = Async.toFuture(Laminar.run(root)(asynchronous(later.future)))
    walk(root, later, result)

  test("Async: the run cancelled while a screen waits for a press — the rest never run, the future Cancelled"):
    val root = fresh()
    val running = Async.start(Laminar.run(root)(asynchronous(Future.successful(1))))
    for
      _ <- settled
      _ = assertEquals(q("h1").textContent, "Who?")
      _ = running.cancel()
      e <- running.future.failed
      _ = q("#f-name").asInstanceOf[dom.html.Input].value = "Ann"
      _ = q("form[data-action=go]").dispatchEvent(new dom.Event("submit",
        new dom.EventInit { bubbles = true; cancelable = true }))
      _ <- settled
    yield
      assert(e.isInstanceOf[Cancelled], e.toString)
      // the face's own form took the press; the program's next screen never came
      assert(!root.textContent.contains("asking for"), root.innerHTML)

  /** a wait the test sees started and cancelled: a `fetch` would be aborted there */
  final class Wait:
    var started = 0
    var cancelled = 0
    val answer = Promise[Int]()
    def program[R[_]](using In[Async, R]): Int ! R = Async.cancellable[Int, R] { cb =>
      started += 1
      answer.future.foreach(n => cb(Right(n)))
      () => cancelled += 1
    }

  /** a block of work with a way out, twice: the second runs whatever the first did */
  def leaving(first: Wait, second: Wait): String ! Paged =
    def once(w: Wait): Either[String, Int] ! Paged =
      interruptible[String, Int, Paged](Button("Stop")("stopped"))(
        working[Paged]("Working")(Part.text("waiting")).andThen(w.program[Paged]))
    for
      a <- once(first)
      _ <- show[Unit, Paged](Screen[Unit]("Between")(Part.text(s"$a"))(Button("Again")(())))
      b <- once(second)
      done <- show[String, Paged](Screen[String]("End")(Part.text(s"$b"))(Button("OK")(s"$a $b")))
    yield done

  test("an exit pressed while the program waits: the wait cancelled, the program on after the block with Left"):
    val root = fresh()
    val (first, second) = (Wait(), Wait())
    val result = Async.toFuture(Laminar.run(root)(leaving(first, second)))
    for
      _ <- settled
      _ = assertEquals((q("h1").textContent, first.started, first.cancelled), ("Working", 1, 0))
      _ = q("button[data-action=stop]").click()
      _ <- settled
      _ = assertEquals((q("h1").textContent, first.cancelled), ("Between", 1))
      _ = assert(root.textContent.contains("Left(stopped)"), root.innerHTML)
      // the first wait's late answer goes nowhere
      _ = first.answer.success(1)
      _ = q("button[data-action=again]").click()
      _ <- settled
      _ = assertEquals((q("h1").textContent, second.started), ("Working", 1))
      _ = second.answer.success(7)
      _ <- settled
      _ = assertEquals(q("h1").textContent, "End")
      _ = assert(root.textContent.contains("Right(7)"), root.innerHTML)
      _ = assertEquals(second.cancelled, 0)
      _ = q("button[data-action=ok]").click()
      done <- result
    yield assertEquals(done, "Left(stopped) Right(7)")

  test("an exit on a screen no longer shown is not taken"):
    val root = fresh()
    val (first, second) = (Wait(), Wait())
    val _ = Async.toFuture(Laminar.run(root)(leaving(first, second)))
    for
      _ <- settled
      stale = q("button[data-action=stop]")
      _ = first.answer.success(3)
      _ <- settled
      _ = assertEquals(q("h1").textContent, "Between")
      _ = stale.click()
      _ <- settled
    yield assertEquals((q("h1").textContent, first.cancelled), ("Between", 0))

  test("deprecated, Lifted[Future]: the same page, lowered by Lift.lower"):
    val root = fresh()
    val later = Promise[Int]()
    val result = Lift.lower(Laminar.run(root)(program(later.future)))
    walk(root, later, result)

  def walk(root: dom.Element, later: Promise[Int], result: Future[String]): Future[Unit] =
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
