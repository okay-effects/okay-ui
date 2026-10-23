package okay.business

import okay.business.model.*

/** the model alone: what fields accept, forms read, screens give — the one checking every toolkit shares */
class ModelSuite extends munit.FunSuite:
  test("days: ISO text, leap years, no 30th of February"):
    assertEquals(Day.parse("2024-02-29"), Right(Day(2024, 2, 29)))
    assertEquals(Day.parse("2026-02-29"), Left("not a day"))
    assertEquals(Day.parse("tomorrow"), Left("a date, yyyy-mm-dd"))

  test("fields read text and typed entries alike; required, optional, rules"):
    assertEquals(Field.money("Amount").read(Entry.Text("12.5")), Right(BigDecimal("12.50")))
    assertEquals(Field.money("Amount").read(Entry.Number(BigDecimal("1.005"))), Left("at most two places"))
    assertEquals(Field.whole("Count").read(Entry.Text("1.5")), Left("a whole number"))
    assertEquals(Field.text("Name").read(Entry.Text("  ")), Left("required"))
    assertEquals(Field.text("Note").optional.read(Entry.Blank), Right(None))
    assertEquals(Field.flag("VIP").read(Entry.Blank), Right(false))
    assertEquals(Field.choice("Size", "Small" -> 1, "Large" -> 2).read(Entry.Text("large")), Right(2))
    assertEquals(Field.choice("Size", "Small" -> 1, "Large" -> 2).read(Entry.Pick("small")), Right(1))
    assertEquals(Field.text("Email").check("an e-mail address")(_.contains("@")).read(Entry.Text("x")),
      Left("an e-mail address"))
    assertEquals(Field.text("Full name").key, "full-name")

  final case class Person(name: String, age: Long)

  test("a form: a tuple of its fields, a case class by `as`, rules across fields, filled with a value"):
    val form = Form(Field.text("Name"), Field.whole("Age")).as[Person].check("adults only")(_.age >= 18)
    val entries = Map("name" -> Entry.Text("Ann"), "age" -> Entry.Text("42"))
    assertEquals(form.read(k => entries.getOrElse(k, Entry.Blank)), Right(Person("Ann", 42)))
    assertEquals(form.read(k => if k == "age" then Entry.Text("7") else entries(k)), Left(List((None, "adults only"))))
    assertEquals(form.read(_ => Entry.Blank), Left(List((Some("name"), "required"), (Some("age"), "required"))))
    assertEquals(form.filledWith(Person("Bob", 30)).fields.map(_.initial),
      List(Entry.Text("Bob"), Entry.Number(BigDecimal(30))))

  test("a screen: a button's result from its form and table; a field not sent is as it started"):
    val form = Form(Field.text("Name"), Field.whole("Age")).as[Person].filledWith(Person("Ann", 42))
    val table = Table(List("a", "b"))(Column.text("Letter")(identity))
    val screen = Screen[Either[Person, String]]("Edit")(form, table)(
      Button.submit("Save", form)(Left(_)), Button.select("Open", table)(Right(_)))
    assertEquals(screen.press(Answer(0, Map((0, "age") -> Entry.Text("43")))), Right(Left(Person("Ann", 43))))
    assertEquals(screen.press(Answer(1, selected = Map(1 -> "2"))), Right(Right("b")))
    assertEquals(screen.press(Answer(1)), Left(List(Problem(Problem.At.Rows(1), "select a row"))))
    assertEquals(screen.press(Answer(0, Map((0, "age") -> Entry.Text("x")))),
      Left(List(Problem(Problem.At.Field(0, "age"), "a number"))))

  test("a button needing a form that is not on its screen is refused when the screen is made"):
    val form = Form.one(Field.text("Name"))
    intercept[IllegalArgumentException](Screen[String]("Nothing")()(Button.submit("Save", form)(identity)))

  test("play: a script by labels, the titles seen, where it stopped and why"):
    val form = Form.one(Field.text("Name"))
    lazy val asked: Step[String] = Step.Waiting(Screen[Option[String]]("Who?")(form)(Button.submit("Go", form)(Some(_)),
      Button("Never mind")(None)), {
      case Some(n) => Step.Done(n)
      case None => asked
    })
    assertEquals(Play(asked, Act.press("Go", "Name" -> "Ann")), Played(Some("Ann"), List("Who?"), Nil, None))
    val stuck = Play(asked, Act.press("Never mind"), Act.press("Go"))
    assertEquals((stuck.result, stuck.seen, stuck.problems.map(_.message)), (None, List("Who?", "Who?"), List("required")))
