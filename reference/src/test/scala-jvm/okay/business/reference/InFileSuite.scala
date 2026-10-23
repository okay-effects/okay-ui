package okay.business.reference

import java.nio.file.Files

/** THE SAME SCRIPT, THE LEDGER IN A FILE: a new file in a temporary directory for each run, the same records */
class InFileSuite extends ExpensesSuite("in a file", () =>
  val file = Ledger.file(Files.createTempDirectory("expenses").resolve("expenses.json"))
  (file, () => file.entries)):

  test("in a file: what was kept is there for the next run, and in the file"):
    val path = Files.createTempDirectory("expenses").resolve("expenses.json")
    val first = Ledger.file(path)
    okay.business.scala3.Play(okay.business.scala3.steps(first(Today.fixed(today)(Expenses.main[Expenses.App]))),
      three*)
    assertEquals(Ledger.file(path).entries, kept)
    assertEquals(Expense.read(Files.readString(path)), kept)
