package okay.business.examples

import okay.business.faces.OnFaces
import okay.business.model.{Step, Day}
import okay.effects.json.Json.{Bool, Num, Str, obj}
import okay.effects.machine.Machine.*
import okay.faces.Chosen
import okay.faces.test.Plays

/** THE SHOP ON OKAY-FACES: the business program's steps, of each kernel, as a program of `Face` — played by okay-faces'
 * own test face, and at its terminal */
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
    test(s"$kernel on okay-faces' test face: a customer, an order chosen by its row, the shop left"):
      val (first, records) = start()
      val played = plays.handle(OnFaces.program(first))(Script.of(Chosen.plain("customers"), Chosen.plain("add"), ann,
        Chosen.plain("ok"), Chosen.plain("back"), Chosen.plain("new-order"),
        Chosen("choose", obj("row" -> Str("Ann | ann@example.com | yes"))),
        Chosen("save", obj("amount" -> Num(12.5), "due" -> Str("2026-11-01"))), Chosen.plain("yes"),
        Chosen.plain("ok"), Chosen.plain("quit")))
      played match
        case Played.Finished(views, ()) => assert(views.exists(_.toString.contains("The order for Ann is saved")))
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

  test("at okay-faces' terminal, by lines typed: the shop on okay-scala"):
    val m = scala3.Shop.Memory()
    val term = okay.faces.term.Terminal()
    val io = new okay.effects.world.Io()
    val lines = List("customers", "add", "save", "Ann", "ann@example.com", "y", "ok", "back", "quit")
    val (printed, result) = run(io.console(lines)(term.handle(OnFaces.program(scala3.Shop.app(m)))))
    assertEquals(result, Some(()), printed.mkString("\n"))
    assertEquals(m.customers.map(_.name), List("Ann"))
    assert(printed.exists(_.contains("Ann added")), printed.mkString("\n"))
