package okay.business.examples

/** README's example, as it is written there */
object Readme:
  import okay.business.scala3.*

  final case class Customer(name: String, email: String, vip: Boolean)

  val customerForm: Form[Customer] =
    Form(Field.text("Name"), Field.text("Email").check("an e-mail address")(_.contains("@")), Field.flag("VIP"))
      .as[Customer]

  def addCustomer[R[_]: Has[Ui]]: Option[Customer] ! R =
    edit("New customer", customerForm).flatMap {
      case Some(c) => inform("Saved", s"${c.name} added").as(Some(c))
      case None => pure(None)
    }

  def played = Play(steps(addCustomer[Ui]), Act.press("Save", "Name" -> "Ann", "Email" -> "ann@example.com"),
    Act.press("OK"))

class ReadmeSuite extends munit.FunSuite:
  test("README's example plays"):
    assertEquals(Readme.played.result, Some(Some(Readme.Customer("Ann", "ann@example.com", false))))
