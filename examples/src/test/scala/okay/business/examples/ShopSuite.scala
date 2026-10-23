package okay.business.examples

import okay.business.model.*

/** THE SHOP BY SCRIPT, the same acts on every kernel: the program is one, its copies differ by the import alone */
abstract class ShopSuite(kernel: String, start: () => (Step[Unit], () => (List[String], List[String]))) extends
    munit.FunSuite:
  def play(acts: Act*): (Played[Unit], (List[String], List[String])) =
    val (first, records) = start()
    (Play(first, acts*), records())

  test(s"$kernel: a customer added, then the shop left"):
    val (played, (customers, _)) = play(Act.press("Customers"), Act.press("Add"),
      Act.press("Save", "Name" -> "Ann", "Email" -> "ann@example.com", "VIP" -> "yes"), Act.press("OK"),
      Act.press("Back"), Act.press("Quit"))
    assertEquals(played.result, Some(()))
    assertEquals(played.seen, List("Shop", "Customers", "New customer", "Saved", "Customers", "Shop"))
    assertEquals(customers, List("Ann <ann@example.com> vip"))

  test(s"$kernel: the form checks — a wrong e-mail stays on the form, the problem at its field"):
    val (played, (customers, _)) = play(Act.press("Customers"), Act.press("Add"),
      Act.press("Save", "Name" -> "Bob", "Email" -> "bob"))
    assertEquals(played.result, None)
    assertEquals(played.at.map(_.title), Some("New customer"))
    assertEquals(played.problems, List(Problem(Problem.At.Field(0, "email"), "an e-mail address")))
    assertEquals(customers, Nil)

  test(s"$kernel: an order — a customer chosen in a table, a form, a confirmation"):
    val (played, (_, orders)) = play(Act.press("Customers"), Act.press("Add"),
      Act.press("Save", "Name" -> "Ann", "Email" -> "ann@example.com"), Act.press("OK"), Act.press("Back"),
      Act.press("New order"), Act.choose("Choose", "Ann"),
      Act.press("Save", "Amount" -> "12.5", "Due" -> "2026-11-01"), Act.press("Yes"), Act.press("OK"),
      Act.press("Quit"))
    assertEquals(played.result, Some(()))
    assertEquals(orders, List("Ann 12.50 2026-11-01 -"))

  test(s"$kernel: what a field accepts is the model's — money of three places, a day that is not, nothing chosen"):
    val (money, _) = play(Act.press("Customers"), Act.press("Add"),
      Act.press("Save", "Name" -> "Ann", "Email" -> "a@b"), Act.press("OK"), Act.press("Back"),
      Act.press("New order"), Act.choose("Choose", "Ann"), Act.press("Save", "Amount" -> "1.005", "Due" -> "2026-02-30"))
    assertEquals(money.problems.map(_.message), List("at most two places", "not a day"))
    val (none, _) = play(Act.press("New order"), Act.press("Choose"))
    assertEquals(none.problems, List(Problem(Problem.At.Rows(0), "select a row")))

  test(s"$kernel: back — an earlier step answered again goes on from there"):
    val (first, _) = start()
    first match
      case w: Step.Waiting[Unit, s] =>
        val toCustomers = w.answer(Answer(0))
        val toOrders = w.answer(Answer(2))
        assertEquals(toCustomers.map { case s: Step.Waiting[?, ?] => s.screen.title; case _ => "" }, Right("Customers"))
        assertEquals(toOrders.map { case s: Step.Waiting[?, ?] => s.screen.title; case _ => "" }, Right("Orders"))
      case other => fail(s"$other")


object Records:
  def customer(name: String, email: String, vip: Boolean): String = s"$name <$email>${if vip then " vip" else ""}"
  def order(customer: String, amount: BigDecimal, due: Day, note: Option[String]): String =
    s"$customer ${amount.bigDecimal.toPlainString} $due ${note.getOrElse("-")}"

class OnScala3Suite extends ShopSuite("okay-scala", () =>
  val m = scala3.Shop.Memory()
  (scala3.Shop.app(m), () => (m.customers.map(c => Records.customer(c.name, c.email, c.vip)),
    m.orders.map(o => Records.order(o.customer, o.amount, o.due, o.note)))))

class OnSimpleSuite extends ShopSuite("okay-simple machine", () =>
  val m = simple.Shop.Memory()
  (simple.Shop.app(m), () => (m.customers.map(c => Records.customer(c.name, c.email, c.vip)),
    m.orders.map(o => Records.order(o.customer, o.amount, o.due, o.note)))))

class OnFreeSuite extends ShopSuite("okay-simple Free", () =>
  val m = free.Shop.Memory()
  (free.Shop.app(m), () => (m.customers.map(c => Records.customer(c.name, c.email, c.vip)),
    m.orders.map(o => Records.order(o.customer, o.amount, o.due, o.note)))))
