package okay.business.examples

import okay.business.faces.OnFaces
import okay.business.model.Step
import okay.effects.json.Json.{Bool, Num, Str, obj}
import okay.effects.cek.OnCek.*
import okay.faces.Chosen
import okay.faces.test.Plays

/** THE SHOP ON OKAY-FACES: the business program's steps, of each kernel, as a program of `Face` — played by okay-faces'
 * own test face, and at its terminal — on okay-scala's machine (`OnCek`) */
class FacesSuite extends munit.FunSuite:
  val plays = Plays()
  import plays.{Played, Script}

  /** the shop on each kernel, and its records as text */
  val kernels: List[(String, () => (Step[Unit], () => (List[String], List[String])))] = List(
    "okay-scala" -> (() =>
      val m = scala3.Shop.Memory()
      (scala3.Shop.app(m), () => (m.customers.map(c => Records.customer(c.name, c.email, c.vip)),
        m.orders.map(o => Records.order(o.customer, o.amount, o.due, o.note))))),
    "okay-simple machine" -> (() =>
      val m = simple.Shop.Memory()
      (simple.Shop.app(m), () => (m.customers.map(c => Records.customer(c.name, c.email, c.vip)),
        m.orders.map(o => Records.order(o.customer, o.amount, o.due, o.note))))),
    "okay-simple Free" -> (() =>
      val m = free.Shop.Memory()
      (free.Shop.app(m), () => (m.customers.map(c => Records.customer(c.name, c.email, c.vip)),
        m.orders.map(o => Records.order(o.customer, o.amount, o.due, o.note))))))

  val ann = Chosen("save", obj("name" -> Str("Ann"), "email" -> Str("ann@example.com"), "vip" -> Bool(true)))

  for (kernel, start) <- kernels do
    test(s"$kernel on okay-faces' test face: a customer, an order chosen by its row, the shop's menu again"):
      val (first, records) = start()
      val played = plays.handle(OnFaces.program(first))(Script.of(Chosen.plain("customers"), Chosen.plain("add"), ann,
        Chosen.plain("ok"), Chosen.plain("back"), Chosen.plain("new-order"),
        Chosen("choose", obj("row" -> Str("Ann | ann@example.com | yes"))),
        Chosen("save", obj("amount" -> Num(12.5), "due" -> Str("2026-11-01"))), Chosen.plain("yes"),
        Chosen.plain("ok")))
      // no quit: the script ends with the shop waiting at its menu
      played match
        case Played.Stuck(views, _, None) =>
          assert(views.exists(_.toString.contains("The order for Ann is saved")))
          assert(views.last.toString.contains("Heading(1,Shop)"), views.last.toString)
        case other => fail(s"$other")
      assertEquals(records(), (List("Ann <ann@example.com> vip"), List("Ann 12.50 2026-11-01 -")))

    test(s"$kernel on okay-faces' test face: a field's problem at the field, the step waiting again"):
      val (first, _) = start()
      plays.handle(OnFaces.program(first))(Script.of(Chosen.plain("customers"), Chosen.plain("add"),
        Chosen("save", obj("name" -> Str("Bob"), "email" -> Str("bob"))))) match
        case Played.Stuck(_, at, Some(rejected)) =>
          assertEquals(rejected.problems.map(p => (p.path, p.message)), List((List("email"), "an e-mail address")))
          assertEquals(at.actions.map(_.name), List("save", "cancel"))
        case other => fail(s"$other")

  test("a working screen on okay-faces: told, and on at once — its link a View.Link"):
    import okay.business.scala3.{!, Has, Ui, Part, Screen, Button, working, show, steps, andThen}
    def p[R[_]: Has[Ui]]: Int ! R = working[R]("Counting")(Part.link("the source", "https://example.org/"))
      .andThen(show(Screen[Int]("Counted")()(Button("OK")(2))))
    plays.handle(OnFaces.program(steps(p[Ui])))(Script.of(Chosen.plain("ok"))) match
      case Played.Finished(views, 2) =>
        assert(views.head.toString.contains("Link(the source,https://example.org/)"), views.head.toString)
      case other => fail(s"$other")

  test("a field held back (Field.more) is its action's more on okay-faces: the same values, Options where drawn"):
    import okay.business.model.{Field, Form, Screen, Button}
    val form = Form(Field.text("Name"), Field.choice("Speed", "Slow" -> "slow", "Fast" -> "fast").initially("slow")
      .more)
    val screen = Screen[Int]("Held")(form)(Button.submit("Go", form)(_ => 1))
    val go = OnFaces.actions(screen).head
    assertEquals(go.more, Set("speed"))
    assertEquals(go.affordance.more, Set("speed"))
    // nothing about its value: the field decodes as before, its initial value its default
    assert(go.taken(obj("name" -> Str("Ann"), "speed" -> Str("Slow"))).isRight)

  test("a picture (Part.Picture) is okay-faces' View.Picture: its shapes, their marks, its words"):
    import okay.business.model.{Part, Screen, Button}
    val pic = Part.picture(100, 40, "5 ETH to Binance")(Part.Shape.Box(0, 0, 10, 40, "start", "start"),
      Part.Shape.Band(10, 20, 90, 20, 12, "exchange", "5 ETH"), Part.Shape.Label(95, 20, "Binance", "end"))
    val view = OnFaces.view(Screen[Int]("Pic")(pic)(Button("OK")(1)), Nil)
    assert(view.toString.contains("Picture(100,40,Vector(Box(0.0,0.0,10.0,40.0,start,start), Band(10.0,20.0,90.0," +
      "20.0,12.0,exchange,5 ETH), Label(95.0,20.0,Binance,end)),5 ETH to Binance)"), view.toString)

  test("at okay-faces' terminal, by lines typed: the shop on okay-scala"):
    val m = scala3.Shop.Memory()
    val term = okay.faces.term.Terminal()
    val io = new okay.effects.world.Io()
    // no quit: the input ends, and the terminal with it
    val lines = List("customers", "add", "save", "Ann", "ann@example.com", "y", "ok", "back")
    val (printed, _) = run(io.console(lines)(term.handle(OnFaces.program(scala3.Shop.app(m)))))
    assertEquals(m.customers.map(_.name), List("Ann"))
    assert(printed.exists(_.contains("Ann added")), printed.mkString("\n"))
