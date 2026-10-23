package okay.business.reference

import java.nio.file.{Files, Path, StandardCopyOption}
import okay.business.scala3.*

/** the JVM's ledger: in a file */
trait LedgerOnPlatform:
  def file(path: Path): InFile = InFile(path)

/** THE LEDGER IN A FILE: JSON, read on each question, rewritten whole on each change (a file beside it, moved over
 * it) — no file yet is no expenses */
final class InFile(path: Path) extends Handles[Ledger]:
  def entries: List[Expense] = if Files.exists(path) then Expense.read(Files.readString(path)) else Nil
  private def keep(es: List[Expense]): Unit =
    val next = path.resolveSibling(s"${path.getFileName}.next")
    Files.writeString(next, Expense.write(es))
    Files.move(next, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

  def apply[R[_], Rest[_], A](p: A ! R)(using Split[Ledger, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Ledger, Rest, A, A](a => pure(a), [X] => (asked: Ledger[X]) => asked match
      case Ledger.Entries() => entries
      case Ledger.Add(e) => keep(entries :+ e)
      case Ledger.Remove(id) => keep(entries.filterNot(_.id == id))))
