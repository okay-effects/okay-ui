package okay.business.examples

import okay.business.scala3.*

/** A WORKING SCREEN: shown while the program goes on, answered at once — the steps run through it, Play counts it seen */
class WorkingSuite extends munit.FunSuite:
  def counting[R[_]: Has[Ui]]: Int ! R =
    working[R]("Counting")(Part.text("1 of 2"), Part.link("the source", "https://example.org/"))
      .andThen(working[R]("Counting")(Part.text("2 of 2")))
      .andThen(show(Screen[Int]("Counted")(Part.text("two"))(Button("OK")(2))))

  test("working screens run through, seen in order; the screen after them waits for its press"):
    val played = Play(steps(counting[Ui]), Act.press("OK"))
    assertEquals(played, Played(Some(2), List("Counting", "Counting", "Counted"), Nil, None))
    steps(counting[Ui]) match
      case Step.Working(screen, _) => assertEquals(screen.parts.last, Part.Link("the source", "https://example.org/"))
      case other => fail(s"$other")
