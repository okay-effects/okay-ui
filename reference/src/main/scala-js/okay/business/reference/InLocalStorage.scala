package okay.business.reference

import okay.business.scala3.*
import org.scalajs.dom

/** the page's ledger: in its storage */
trait LedgerOnPlatform:
  def localStorage(key: String): InLocalStorage = InLocalStorage(key)

/** THE LEDGER IN THE PAGE'S STORAGE: `window.localStorage[key]`, JSON, read on each question, rewritten on each change
 * — kept across reloads; nothing there, or something unreadable, is no expenses */
final class InLocalStorage(key: String) extends Handles[Ledger]:
  def entries: List[Expense] = Expense.read(dom.window.localStorage.getItem(key))
  private def keep(es: List[Expense]): Unit = dom.window.localStorage.setItem(key, Expense.write(es))

  def apply[R[_], Rest[_], A](p: A ! R)(using Split[Ledger, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Ledger, Rest, A, A](a => pure(a), [X] => (asked: Ledger[X]) => asked match
      case Ledger.Entries() => entries
      case Ledger.Add(e) => keep(entries :+ e)
      case Ledger.Remove(id) => keep(entries.filterNot(_.id == id))))
