package okay.business.examples.scala3

// THE ONE LINE that says the kernel: the build makes this file's copies for okay-simple's machine and Free by
// changing it (and the package) alone
import okay.business.scala3.*

final case class Customer(name: String, email: String, vip: Boolean)
final case class Order(customer: String, amount: BigDecimal, due: Day, note: Option[String])

/** the application's own effect: its records, kept by a handler of its choice */
enum Book[X]:
  case Customers() extends Book[List[Customer]]
  case AddCustomer(c: Customer) extends Book[Unit]
  case Orders() extends Book[List[Order]]
  case AddOrder(o: Order) extends Book[Unit]

/** A SHOP: customers and their orders, in a business user's words — a menu, tables, forms, buttons */
object Shop:
  type App[X] = (Ui + Book)[X]

  def customerForm: Form[Customer] =
    Form(Field.text("Name"), Field.text("Email").check("an e-mail address")(_.contains("@")), Field.flag("VIP"))
      .as[Customer]
  def orderForm: Form[(BigDecimal, Day, Option[String])] =
    Form(Field.money("Amount").check("more than zero")(_ > 0), Field.date("Due"), Field.longText("Note").optional)

  def customersOf(cs: List[Customer]): Table[Customer] =
    Table(cs)(Column.text("Name")(_.name), Column.text("Email")(_.email), Column.flag("VIP")(_.vip))
      .keyedBy(_.email).whenEmpty("no customers yet")

  enum Go:
    case Add
    case Open(c: Customer)
    case Back

  /** the shop, until Quit */
  def main[R[_]: Has[Ui]: Has[Book]]: Unit ! R =
    menu[Int, R]("Shop", "Customers" -> 1, "New order" -> 2, "Orders" -> 3, "Quit" -> 0).flatMap {
      case 0 => pure(())
      case 1 => customers[R].andThen(defer(main[R]))
      case 2 => newOrder[R].andThen(defer(main[R]))
      case _ => orders[R].andThen(defer(main[R]))
    }

  def customers[R[_]: Has[Ui]: Has[Book]]: Unit ! R =
    perform[Book](Book.Customers()).flatMap { cs =>
      val table = customersOf(cs)
      show(Screen[Go]("Customers")(Part.text(s"${cs.size} customers"), table)(
        Button.select("Open", table)(Go.Open(_)), Button("Add")(Go.Add), Button("Back")(Go.Back))).flatMap {
        case Go.Back => pure(())
        case Go.Open(c) =>
          show(Screen[Unit](c.name)(Part.details("Name" -> c.name, "Email" -> c.email, "VIP" -> Cell.Flag(c.vip)))(
            Button("Back")(()))).andThen(defer(customers[R]))
        case Go.Add =>
          edit[Customer, R]("New customer", customerForm).flatMap {
            case None => defer(customers[R])
            case Some(c) =>
              perform[Book](Book.AddCustomer(c)).andThen(inform[R]("Saved", s"${c.name} added"))
                .andThen(defer(customers[R]))
          }
      }
    }

  def newOrder[R[_]: Has[Ui]: Has[Book]]: Unit ! R =
    perform[Book](Book.Customers()).flatMap { cs =>
      pick[Customer, R]("New order: whose?", customersOf(cs)).flatMap {
        case None => pure(())
        case Some(c) =>
          edit[(BigDecimal, Day, Option[String]), R](s"New order for ${c.name}", orderForm,
            Part.details("Customer" -> c.name, "Email" -> c.email)).flatMap {
            case None => pure(())
            case Some((amount, due, note)) =>
              confirm[R](s"Save an order of $amount EUR for ${c.name}, due $due?").flatMap { yes =>
                if !yes then pure(())
                else perform[Book](Book.AddOrder(Order(c.name, amount, due, note)))
                  .andThen(inform[R]("Saved", s"The order for ${c.name} is saved"))
              }
          }
      }
    }

  def orders[R[_]: Has[Ui]: Has[Book]]: Unit ! R =
    perform[Book](Book.Orders()).flatMap { os =>
      show(Screen[Unit]("Orders")(Table(os)(Column.text("Customer")(_.customer), Column.money("Amount")(_.amount),
        Column.date("Due")(_.due), Column.text("Note")(_.note.getOrElse(""))).whenEmpty("no orders yet"))(
        Button("Back")(())))
    }

  /** the records in memory: a handler of the application's choice, answering in place */
  final class Memory(var customers: List[Customer] = Nil, var orders: List[Order] = Nil):
    def keep[R[_], Rest[_], A](p: A ! R)(using Split[Book, R, Rest]): A ! Rest =
      handle(p, Handler.Tail[Book, Rest, A, A](a => pure(a), [X] => (op: Book[X]) => op match
        case Book.Customers() => customers
        case Book.AddCustomer(c) => customers = customers :+ c
        case Book.Orders() => orders
        case Book.AddOrder(o) => orders = orders :+ o))

  /** the shop in steps, its records in `memory`: what a toolkit drives */
  def app(memory: Memory): Step[Unit] = steps(memory.keep(main[App]))
