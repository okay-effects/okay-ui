package okay.business.reference

import okay.business.scala3.Day

/** the format the ledgers that keep text share: what is written reads back; nothing, or nonsense, is no expenses */
class FormatSuite extends munit.FunSuite:
  val es = List(Expense(1, Day(2026, 10, 3), BigDecimal("12.50"), "Food", Some("lunch, \"two\"")),
    Expense(2, Day(2026, 10, 23), BigDecimal("2.80"), "Transport", None))

  test("written, read back"):
    assertEquals(Expense.read(Expense.write(es)), es)

  test("no text, or text that is not a list of expenses, is no expenses"):
    for text <- List(null, "", "  ", "not json", "{\"id\": 1}", "[1, 2") do assertEquals(Expense.read(text), Nil)

  test("a record that is not one is skipped, the others kept"):
    val text = s"""[${Expense.write(es.take(1)).drop(1).dropRight(1)}, {"id": "x"}, 3,
      {"id": 2, "day": "2026-02-30", "amount": 1, "category": "Food"}]"""
    assertEquals(Expense.read(text), es.take(1))
