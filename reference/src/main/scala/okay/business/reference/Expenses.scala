package okay.business.reference

import okay.business.scala3.*

/**
 * EXPENSES, the reference application: a menu, a form, tables, a confirmation — in okay-ui's words alone. What it
 * knows of the world it asks as effects: its records of `Ledger`, the date of `Today`, the person of `Ui`. Which
 * handlers answer is the line that runs it (specs/reference.md).
 */
object Expenses:
  /** the row of the whole application */
  type App[X] = (Ui + Ledger + Today)[X]

  val categories: List[String] = List("Food", "Transport", "Home", "Health", "Fun", "Other")

  /** a new expense: the day starts at today */
  def form(today: Day): Form[(BigDecimal, String, Day, Option[String])] =
    Form(Field.money("Amount").check("more than zero")(_ > 0), Field.choice("Category", categories.map(c => c -> c)*),
      Field.date("Day").initially(today), Field.text("Note").optional)

  def table(es: List[Expense]): Table[Expense] =
    Table(es)(Column.date("Day")(_.day), Column.text("Category")(_.category), Column.money("Amount")(_.amount),
      Column.text("Note")(_.note.getOrElse(""))).keyedBy(_.id.toString).whenEmpty("no expenses")

  /** the application, for as long as it is open: no Quit — in a page it would only leave the page empty, and a
   * terminal ends the way a terminal does */
  def main[R[_]: Has[Ui]: Has[Ledger]: Has[Today]]: Unit ! R =
    menu[Int, R]("Expenses", "Add an expense" -> 1, "This month" -> 2, "All expenses" -> 3).flatMap {
      case 1 => add[R].andThen(defer(main[R]))
      case 2 => thisMonth[R].andThen(defer(main[R]))
      case _ => all[R].andThen(defer(main[R]))
    }

  def add[R[_]: Has[Ui]: Has[Ledger]: Has[Today]]: Unit ! R =
    perform[Today](Today.Now()).flatMap { today =>
      edit[(BigDecimal, String, Day, Option[String]), R]("Add an expense", form(today)).flatMap {
        case None => pure(())
        case Some((amount, category, day, note)) =>
          perform[Ledger](Ledger.Entries()).flatMap { es =>
            val id = es.map(_.id).maxOption.getOrElse(0L) + 1
            perform[Ledger](Ledger.Add(Expense(id, day, amount, category, note)))
              .andThen(inform[R]("Saved", s"${money(amount)} for $category on $day"))
          }
      }
    }

  def thisMonth[R[_]: Has[Ui]: Has[Ledger]: Has[Today]]: Unit ! R =
    perform[Today](Today.Now()).flatMap { today =>
      perform[Ledger](Ledger.Entries()).flatMap { es =>
        val month = es.filter(e => e.day.year == today.year && e.day.month == today.month).sortBy(_.day)
        val totals = categories.flatMap(c => month.filter(_.category == c).map(_.amount).reduceOption(_ + _)
          .map(c -> _))
        val byCategory = Table(totals)(Column.text("Category")(_._1), Column.money("Total")(_._2))
          .keyedBy(_._1).whenEmpty("nothing this month")
        val heading = f"${today.year}%04d-${today.month}%02d: ${count(month.size)}, ${money(month.map(_.amount).sum)}"
        show(Screen[Unit]("This month")(Part.text(heading), table(month), Part.section("By category")(byCategory))(
          Button("Back")(())))
      }
    }

  def all[R[_]: Has[Ui]: Has[Ledger]]: Unit ! R =
    perform[Ledger](Ledger.Entries()).flatMap { es =>
      val rows = table(es.sortBy(e => (e.day, e.id)).reverse)
      show(Screen[Option[Expense]]("All expenses")(Part.text(count(es.size)), rows)(
        Button.select("Open", rows)(Some(_)), Button("Back")(None))).flatMap {
        case None => pure(())
        case Some(e) => open[R](e).andThen(defer(all[R]))
      }
    }

  /** an expense's details; removed after a confirmation */
  def open[R[_]: Has[Ui]: Has[Ledger]](e: Expense): Unit ! R =
    show(Screen[Boolean](s"Expense ${e.id}")(Part.details("Day" -> Cell.Date(e.day),
      "Category" -> Cell.Text(e.category), "Amount" -> Cell.Money(e.amount, "EUR"),
      "Note" -> Cell.Text(e.note.getOrElse(""))))(
      Button("Remove", Style.Danger)(true), Button("Back")(false))).flatMap { remove =>
      if !remove then pure(())
      else confirm[R](s"Remove ${money(e.amount)} for ${e.category} on ${e.day}?").flatMap { yes =>
        if yes then perform[Ledger](Ledger.Remove(e.id)) else pure(())
      }
    }

  def money(n: BigDecimal): String = Cell.Money(n, "EUR").text
  def count(n: Int): String = if n == 1 then "1 expense" else s"$n expenses"
