package okay.business.faces

import okay.business.model.*
import okay.faces.View

/** LIVE SCREENS on okay-faces (specs/business.md): blocks under marks, buttons at slots as refs, long text, events */
class LiveSuite extends munit.FunSuite:
  val note = Form.one(Field.longText("Note"))
  val screen: Screen[String] = Screen[String]("Calls")(
    Part.block("card", "get_sum")(Part.text("a = 500"), Part.slot("call-1")),
    note)(
    Button("Approve", Style.Primary)("approved").at("call-1"),
    Button("Refuse", Style.Danger)("refused").at("call-1"),
    Button.submit("Send", note)(n => s"note: $n"),
    Button("Elsewhere")("nowhere").at("no-such-slot"))

  test("a block is a marked group; the buttons at a slot are refs there, marked by their style"):
    val v = OnFaces.view(screen, Nil)
    val card = v match
      case View.Group(_, children) => children.collectFirst { case m @ View.Marked("card", _) => m }
      case _ => None
    card match
      case Some(View.Marked(_, View.Group(Some("get_sum"), inner))) =>
        assertEquals(inner.head, View.text("a = 500"))
        assertEquals(inner(1), View.Marked("slot", View.Group(None, Vector(
          View.Marked("button-primary", View.Marked("button", View.Ref("call-1/approve", "Approve"))),
          View.Marked("button-danger", View.Marked("button", View.Ref("call-1/refuse", "Refuse")))))))
      case other => fail(s"no card: $other")

  test("the bar: the buttons not at a slot; a ref chooses the ones at slots; long text is long"):
    val actions = OnFaces.actions(screen)
    assertEquals(actions.map(_.name), List("send", "elsewhere", "ref"))
    assertEquals(actions.head.long, Set("note"))
    val ref = actions.last
    val chosen = ref.taken(okay.effects.json.Json.obj("key" -> okay.effects.json.Json.Str("call-1/refuse")))
    assertEquals(chosen.map(a => screen.press(a)), Right(Right("refused")))

  test("events: a screen until events is answered by one, or by a button; merged events both answer"):
    val a = Events.manual[String]
    val b = Events.manual[String]
    val s = screen.until(a.events).until(b.events)
    var got = List.empty[String]
    val stop = s.events.get.register(x => got :+= x)
    a.fire("one"); b.fire("two")
    stop()
    a.fire("three")
    assertEquals(got, List("one", "two"))
    assertEquals((a.listening, b.listening), (0, 0))

  test("a row's buttons: a slot cell of a table holds the buttons at its name, chosen by a ref as any slot's"):
    final case class Row(name: String, slot: String)
    val rows = List(Row("Vesting", "row-1"), Row("Escrow", "row-2"))
    val table = Table(rows)(Column.text("Case")(_.name), Column.slot("")(_.slot))
    val s = Screen[String]("Examples")(table)(rows.flatMap(r => List(
      Button(s"Step", Style.Primary)(s"step ${r.name}").at(r.slot),
      Button(s"Source")(s"source ${r.name}").at(r.slot)))*)
    OnFaces.view(s, Nil) match
      case View.Group(_, children) => children.collectFirst { case t: View.Table => t } match
          case Some(t) =>
            assertEquals(t.rows(1)(1), View.Marked("slot", View.Group(None, Vector(
              View.Marked("button-primary", View.Marked("button", View.Ref("row-2/step", "Step"))),
              View.Marked("button-plain", View.Marked("button", View.Ref("row-2/source", "Source")))))))
          case None => fail(s"no table: $children")
      case other => fail(s"not a group: $other")
    val ref = OnFaces.actions(s).last
    val chosen = ref.taken(okay.effects.json.Json.obj("key" -> okay.effects.json.Json.Str("row-2/source")))
    assertEquals(chosen.map(a => s.press(a)), Right(Right("source Escrow")))

  test("a cell with a word too long for a phone is marked long; ordinary words are not"):
    val t = Table(List("apply, argument next", "#" + "0" * 40))(Column.text("Holds")(identity))
    OnFaces.view(Screen[Unit]("Cells")(t)(), Nil) match
      case View.Group(_, children) => children.collectFirst { case v: View.Table => v } match
          case Some(v) =>
            assertEquals(v.rows(0)(0), View.text("apply, argument next"))
            val zw = "\u200b"
            assertEquals(v.rows(1)(0), View.Marked("long", View.text(s"#$zw${"0" * 20}$zw${"0" * 20}$zw")))
            assertEquals(OnFaces.breakable("a_b.c"), s"a_${zw}b.${zw}c")
            assertEquals(OnFaces.breakable("ScriptContext"), s"Script${zw}Context")
          case None => fail("no table")
      case other => fail(s"not a group: $other")
