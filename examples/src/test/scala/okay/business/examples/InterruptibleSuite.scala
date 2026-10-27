package okay.business.examples

import okay.business.scala3.*

/** A WAY OUT OF WORK under `steps` and `Play` (specs/business.md, level 3): inside an interruptible block every
 * working screen has the block's exits; the script presses one, and the program goes on after the block with it */
class InterruptibleSuite extends munit.FunSuite:
  enum Out:
    case Stopped, Elsewhere

  /** three working screens, then a question: the work, with a way out */
  def work[R[_]: Has[Ui]]: String ! R =
    working[R]("Reading")(Part.text("1 of 3"))
      .andThen(working[R]("Reading")(Part.text("2 of 3")))
      .andThen(working[R]("Reading")(Part.text("3 of 3")))
      .andThen(show(Screen[String]("Read")(Part.text("all three"))(Button("OK")("read"))))

  def program[R[_]: Has[Ui]]: String ! R =
    interruptible[Out, String, R](Button("Stop")(Out.Stopped))(work[R]).flatMap {
      case Right(read) => pure(read)
      case Left(out) => show(Screen[String]("Stopped")(Part.text(s"$out"))(Button("OK")("stopped")))
    }

  test("no exit pressed: the working screens go on, the block's value Right"):
    assertEquals(Play(steps(program[Ui]), Act.press("OK")),
      Played(Some("read"), List("Reading", "Reading", "Reading", "Read"), Nil, None))

  test("an exit pressed on a working screen: the block left there, the program after it with Left"):
    val played = Play(steps(program[Ui]), Act.leave("Stop"), Act.press("OK"))
    assertEquals(played, Played(Some("stopped"), List("Reading", "Stopped"), Nil, None))

  test("inside a block a working screen is Leavable, its buttons the exits; outside, a Working with none"):
    steps(program[Ui]) match
      case Step.Leavable(screen, _, _) => assertEquals(screen.buttons.map(_.label), List("Stop"))
      case other => fail(s"$other")
    steps(work[Ui]) match
      case Step.Working(screen, _) => assertEquals(screen.buttons, Nil)
      case other => fail(s"$other")

  test("a show inside the block waits for its own buttons: an exit is not one of them"):
    val played = Play(steps(program[Ui]), Act.press("OK"), Act.leave("Stop"))
    assertEquals(played.result, Some("read"))

  test("blocks nested: the innermost's exits first; each leaves its own block"):
    def nested[R[_]: Has[Ui]]: String ! R =
      interruptible[Out, String, R](Button("Elsewhere")(Out.Elsewhere))(
        interruptible[Out, String, R](Button("Stop")(Out.Stopped))(working[R]("Deep")().as("deep"))
          .flatMap(inner => working[R]("Shallow")().as(s"inner $inner")))
        .map(outer => s"outer $outer")
    steps(nested[Ui]) match
      case Step.Leavable(screen, _, _) => assertEquals(screen.buttons.map(_.label), List("Stop", "Elsewhere"))
      case other => fail(s"$other")
    assertEquals(Play(steps(nested[Ui]), Act.leave("Stop")).result, Some("outer Right(inner Left(Stopped))"))
    assertEquals(Play(steps(nested[Ui]), Act.leave("Elsewhere")).result, Some("outer Left(Elsewhere)"))
    // after the inner block: only the outer's exit
    steps(nested[Ui]) match
      case Step.Leavable(_, next, _) => next() match
        case Step.Leavable(screen, _, _) =>
          assertEquals((screen.title, screen.buttons.map(_.label)), ("Shallow", List("Elsewhere")))
        case other => fail(s"$other")
      case other => fail(s"$other")
    assertEquals(Play(steps(nested[Ui])).result, Some("outer Right(inner Right(deep))"))
