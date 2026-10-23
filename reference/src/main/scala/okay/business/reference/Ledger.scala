package okay.business.reference

import okay.business.scala3.*
import okay.effects.json.{Descent, Json}

final case class Expense(id: Long, day: Day, amount: BigDecimal, category: String, note: Option[String])

/** THE FORMAT the ledgers that keep text share: a JSON list of records */
object Expense:
  def write(es: List[Expense]): String = Json.print(Json.Arr(es.toVector.map(e => Json.obj("id" -> Json.num(e.id),
    "day" -> Json.str(e.day.toString), "amount" -> Json.Num(e.amount), "category" -> Json.str(e.category),
    "note" -> e.note.fold[Json](Json.Null)(Json.str)))))

  /** what was kept, read back: no text, or text that is not a list, is no expenses; a record that is not one is
   * skipped */
  def read(text: String): List[Expense] =
    if text == null || text.trim.isEmpty then Nil
    else Descent.parse(text) match
      case Right(Json.Arr(items)) => items.toList.flatMap(record)
      case _ => Nil

  private def record(j: Json): Option[Expense] = j match
    case o: Json.Obj =>
      for
        case Json.Num(id) <- o.get("id") if id.isValidLong
        case Json.Str(day) <- o.get("day")
        day <- Day.parse(day).toOption
        case Json.Num(amount) <- o.get("amount")
        case Json.Str(category) <- o.get("category")
      yield Expense(id.toLong, day, amount, category, o.get("note").collect { case Json.Str(s) => s })
    case _ => None

/** THE APPLICATION'S RECORDS, as an effect: what the program asks of them. Where they are kept is a handler's */
enum Ledger[X]:
  case Entries() extends Ledger[List[Expense]]
  case Add(e: Expense) extends Ledger[Unit]
  case Remove(id: Long) extends Ledger[Unit]

/** the ledgers: in memory here; in a file on the JVM, in the page's storage with Scala.js (`LedgerOnPlatform`) */
object Ledger extends LedgerOnPlatform:
  def inMemory(initial: List[Expense]): InMemory = InMemory(initial)

/** THE LEDGER IN MEMORY: a variable, answered in place — for tests, or a session that need not last */
final class InMemory(initial: List[Expense]) extends Handles[Ledger]:
  var entries: List[Expense] = initial
  def apply[R[_], Rest[_], A](p: A ! R)(using Split[Ledger, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Ledger, Rest, A, A](a => pure(a), [X] => (asked: Ledger[X]) => asked match
      case Ledger.Entries() => entries
      case Ledger.Add(e) => entries = entries :+ e
      case Ledger.Remove(id) => entries = entries.filterNot(_.id == id)))
