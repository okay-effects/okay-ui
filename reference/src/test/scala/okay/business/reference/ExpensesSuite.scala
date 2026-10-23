package okay.business.reference

import okay.business.scala3.*
import okay.business.reference.Expenses.App

/**
 * EXPENSES BY SCRIPT: one script, one set of expected records, for every ledger. A suite of this class is the
 * script against one handler of `Ledger` — in memory here, in a file on the JVM, in the page's storage with Scala.js.
 * Nothing is mocked: a test is the application's line with its handlers chosen.
 */
abstract class ExpensesSuite(ledger: String, fresh: () => (Handles[Ledger], () => List[Expense])) extends
    munit.FunSuite:
  val today: Day = Day(2026, 10, 23)

  /** the whole application, its ledger `fresh`, the day fixed, played by `acts`: how it went, and what was kept */
  def play(acts: Act*): (Played[Unit], List[Expense]) =
    val (handler, kept) = fresh()
    (Play(steps(handler(Today.fixed(today)(Expenses.main[App]))), acts*), kept())

  def add(entries: (String, String)*): List[Act] =
    List(Act.press("Add an expense"), Act.press("Save", entries*), Act.press("OK"))
  val three: List[Act] = add("Amount" -> "12.5", "Category" -> "Food", "Day" -> "2026-10-03", "Note" -> "lunch") ++
    add("Amount" -> "2.80", "Category" -> "Transport") ++
    add("Amount" -> "7.5", "Category" -> "food", "Day" -> "2026-09-30", "Note" -> "groceries")
  val kept: List[Expense] = List(Expense(1, Day(2026, 10, 3), BigDecimal("12.50"), "Food", Some("lunch")),
    Expense(2, today, BigDecimal("2.80"), "Transport", None),
    Expense(3, Day(2026, 9, 30), BigDecimal("7.50"), "Food", Some("groceries")))

  /** the rows of a screen's tables, as text */
  def tables(screen: Option[Screen[?]]): List[List[List[String]]] =
    screen.toList.flatMap(_.inputs.collect { case t: Table[r] => t.rows.map(row => t.cells(row).map(_.text)) })

  test(s"$ledger: expenses added, the day today unless given, then the application left"):
    val (played, records) = play((three :+ Act.press("Quit"))*)
    assertEquals(played.result, Some(()))
    assertEquals(played.seen.take(4), List("Expenses", "Add an expense", "Saved", "Expenses"))
    assertEquals(records, kept)

  test(s"$ledger: this month — its expenses by day, the totals by category; September's not in it"):
    val (played, _) = play((three :+ Act.press("This month"))*)
    assertEquals(played.at.map(_.title), Some("This month"))
    assertEquals(played.at.toList.flatMap(_.parts).collect { case Part.Text(s) => s },
      List("2026-10: 2 expenses, 15.30 EUR"))
    assertEquals(tables(played.at), List(
      List(List("2026-10-03", "Food", "12.50 EUR", "lunch"), List("2026-10-23", "Transport", "2.80 EUR", "")),
      List(List("Food", "12.50 EUR"), List("Transport", "2.80 EUR"))))

  test(s"$ledger: a rejected amount stays on the form, its problem at its field, nothing kept"):
    for (amount, problem) <- List("0" -> "more than zero", "1.005" -> "at most two places", "" -> "required") do
      val (played, records) = play(Act.press("Add an expense"), Act.press("Save", "Amount" -> amount,
        "Category" -> "Food"))
      assertEquals(played.at.map(_.title), Some("Add an expense"))
      assertEquals(played.problems, List(Problem(Problem.At.Field(0, "amount"), problem)))
      assertEquals(records, Nil)

  test(s"$ledger: removed after a confirmation — No keeps it, Yes removes it"):
    val open = List(Act.press("All expenses"), Act.choose("Open", "lunch"), Act.press("Remove"))
    val (kept1, records1) = play((three ++ open :+ Act.press("No"))*)
    assertEquals(kept1.seen.takeRight(4), List("All expenses", "Expense 1", "Confirm", "All expenses"))
    assertEquals(records1, kept)
    val (removed, records2) = play((three ++ open ++ List(Act.press("Yes"), Act.press("Back"), Act.press("Quit")))*)
    assertEquals(removed.result, Some(()))
    assertEquals(records2, kept.tail)

  test(s"$ledger: all expenses, newest first; opening needs a row"):
    val (played, _) = play((three ++ List(Act.press("All expenses"), Act.press("Open")))*)
    assertEquals(tables(played.at).flatten.map(_.head), List("2026-10-23", "2026-10-03", "2026-09-30"))
    assertEquals(played.problems, List(Problem(Problem.At.Rows(0), "select a row")))

class InMemorySuite extends ExpensesSuite("in memory", () =>
  val memory = Ledger.inMemory(Nil)
  (memory, () => memory.entries))
