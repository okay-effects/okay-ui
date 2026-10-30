package okay.business.faces

import okay.business.model.*

/** A file field is long text to a face; a form of fields known only at run time reads into a list */
class FileAndListSuite extends munit.FunSuite:
  test("Field.file: required text, long to the face, named among the screen's files"):
    val form = Form.one(Field.file("Blueprint", ".json"))
    val screen = Screen[String]("Open")(form)(Button.submit("Open", form)(identity))
    assertEquals(OnFaces.files(screen), List("blueprint" -> ".json"))
    assertEquals(screen.press(Answer(0, Map((0, "blueprint") -> Entry.Text("{}")))), Right("{}"))
    assert(screen.press(Answer(0)).isLeft)

  test("Form.list: the values in order; a problem at its field"):
    val deadline = Field.text("deadline").check("a whole number")(_.toLongOption.isDefined)
    val form = Form.list(List(Field.text("owner"), deadline))
    val screen = Screen[List[String]]("Datum")(form)(Button.submit("Run", form)(identity))
    val ok = Answer(0, Map((0, "owner") -> Entry.Text("ab"), (0, "deadline") -> Entry.Text("7")))
    assertEquals(screen.press(ok), Right(List("ab", "7")))
    assert(screen.press(Answer(0, Map((0, "owner") -> Entry.Text("ab"), (0, "deadline") -> Entry.Text("x")))).isLeft)
