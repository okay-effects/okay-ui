package okay.business

import scala.deriving.Mirror

/**
 * THE MODEL: a business application's screens as data — fields, forms, tables, buttons, screens — in the words its
 * users know, and nothing of a toolkit. Every adapter exports it (`import okay.business.scala3.*`), so a program
 * names no package of its own.
 */
object model:

  // ---- VALUES

  /** a date, as people write it: `2026-10-23`. Not `java.time`: the model is for Scala.js too */
  final case class Day(year: Int, month: Int, day: Int):
    override def toString: String = f"$year%04d-$month%02d-$day%02d"

  object Day:
    private def days(y: Int, m: Int): Int = m match
      case 2 => if (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 then 29 else 28
      case 4 | 6 | 9 | 11 => 30
      case _ => 31
    def of(y: Int, m: Int, d: Int): Either[String, Day] =
      if m >= 1 && m <= 12 && d >= 1 && d <= days(y, m) then Right(Day(y, m, d)) else Left("not a day")
    def parse(s: String): Either[String, Day] = s.trim match
      case s"$y-$m-$d" => (y.toIntOption, m.toIntOption, d.toIntOption) match
          case (Some(y), Some(m), Some(d)) => of(y, m, d)
          case _ => Left("a date, yyyy-mm-dd")
      case _ => Left("a date, yyyy-mm-dd")
    given Ordering[Day] = Ordering.by(d => (d.year, d.month, d.day))

  /** WHAT A PERSON ENTERED in a field, as a toolkit reads it: typed where the toolkit has typed widgets, text where it
   * has only text — every field accepts text and parses it */
  enum Entry:
    case Blank
    case Text(s: String)
    case Number(n: BigDecimal)
    case Date(d: Day)
    case Flag(b: Boolean)
    /** a choice, by its key */
    case Pick(key: String)

    /** as text, for a toolkit of text */
    def text: String = this match
      case Blank => ""
      case Text(s) => s
      case Number(n) => n.bigDecimal.toPlainString
      case Date(d) => d.toString
      case Flag(b) => if b then "yes" else "no"
      case Pick(k) => k

    def isBlank: Boolean = this match
      case Blank => true
      case Text(s) => s.trim.isEmpty
      case _ => false

  // ---- FIELDS AND FORMS

  /** what a field holds, for a toolkit to choose its widget */
  enum Kind:
    case Text, LongText, Secret, Whole, Decimal, Date, Flag
    case Money(currency: String)
    /** the options' keys and labels */
    case Choice(options: List[(String, String)])

  /**
   * A FIELD of a form: its key (unique in its form, made from the label unless given), its label, its kind, what it
   * starts with, and how it reads an `Entry` — the one checking of every toolkit.
   */
  final case class Field[A](key: String, label: String, kind: Kind, hint: Option[String], initial: Entry,
    read: Entry => Either[String, A], write: A => Entry, required: Boolean, held: Boolean = false):
    def keyed(k: String): Field[A] = copy(key = k)
    def hinted(h: String): Field[A] = copy(hint = Some(h))
    /** one a person rarely changes, its initial value right for most: a toolkit may hold it back, under "Options" */
    def more: Field[A] = copy(held = true)
    /** starting with `a` */
    def initially(a: A): Field[A] = copy(initial = write(a))
    /** a rule of the business: `message` when `ok` fails */
    def check(message: String)(ok: A => Boolean): Field[A] =
      copy(read = e => read(e).flatMap(a => if ok(a) then Right(a) else Left(message)))
    /** may be left blank: `None` then */
    def optional: Field[Option[A]] =
      Field[Option[A]](key, label, kind, hint, initial, e => if e.isBlank then Right(None) else read(e).map(Some(_)),
        _.fold(Entry.Blank)(write), required = false, held = held)

  object Field:
    /** a key from a label: lowercase, dashes for spaces, the rest dropped */
    def keyOf(label: String): String =
      val k = label.trim.toLowerCase.replaceAll("\\s+", "-").replaceAll("[^a-z0-9_-]", "").dropWhile(c => !c.isLetter)
      if k.isEmpty then "field" else k

    private def required[A](label: String, kind: Kind, write: A => Entry)(read: Entry => Either[String, A]): Field[A] =
      Field(keyOf(label), label, kind, None, Entry.Blank, e => if e.isBlank then Left("required") else read(e), write,
        required = true)

    private def number(e: Entry): Either[String, BigDecimal] = e match
      case Entry.Number(n) => Right(n)
      case other => scala.util.Try(BigDecimal(other.text.trim)).toOption.toRight("a number")

    def text(label: String): Field[String] = required[String](label, Kind.Text, Entry.Text(_))(e => Right(e.text.trim))
    def longText(label: String): Field[String] = required[String](label, Kind.LongText, Entry.Text(_))(e => Right(e.text))
    def secret(label: String): Field[String] = required[String](label, Kind.Secret, Entry.Text(_))(e => Right(e.text))
    def whole(label: String): Field[Long] = required[Long](label, Kind.Whole, n => Entry.Number(BigDecimal(n)))(e =>
      number(e).flatMap(n => if n.isWhole && n.isValidLong then Right(n.toLong) else Left("a whole number")))
    def decimal(label: String): Field[BigDecimal] = required[BigDecimal](label, Kind.Decimal, Entry.Number(_))(number)
    /** an amount of money: at most two places */
    def money(label: String, currency: String = "EUR"): Field[BigDecimal] =
      required[BigDecimal](label, Kind.Money(currency), Entry.Number(_))(e =>
        number(e).flatMap(n => if n.scale <= 2 then Right(n.setScale(2)) else Left("at most two places")))
    def date(label: String): Field[Day] = required[Day](label, Kind.Date, Entry.Date(_)) {
      case Entry.Date(d) => Right(d)
      case other => Day.parse(other.text)
    }
    /** yes or no: blank is no */
    def flag(label: String): Field[Boolean] =
      Field(keyOf(label), label, Kind.Flag, None, Entry.Flag(false), {
        case Entry.Flag(b) => Right(b)
        case e if e.isBlank => Right(false)
        case e => e.text.trim.toLowerCase match
            case "yes" | "y" | "true" => Right(true)
            case "no" | "n" | "false" => Right(false)
            case _ => Left("yes or no")
      }, b => Entry.Flag(b), required = false)
    /** one of the options, each a label and its value; chosen by key or by label */
    def choice[A](label: String, options: (String, A)*): Field[A] =
      val keyed = options.toList.map((l, a) => (keyOf(l), l, a))
      required[A](label, Kind.Choice(keyed.map((k, l, _) => (k, l))),
        a => keyed.find(_._3 == a).fold(Entry.Blank)(o => Entry.Pick(o._1))) { e =>
        val t = e.text.trim
        keyed.find((k, l, _) => k == t || l.equalsIgnoreCase(t)).map(_._3)
          .toRight(s"one of ${keyed.map(_._2).mkString(", ")}")
      }

  /** a problem with what was entered: where, and what */
  final case class Problem(at: Problem.At, message: String)
  object Problem:
    enum At:
      /** the screen as a whole: a rule across fields, a button */
      case Screen
      /** a field of the form that is the screen's input `input` */
      case Field(input: Int, key: String)
      /** the rows of the table that is the screen's input `input` */
      case Rows(input: Int)
  type Problems = List[Problem]

  // ---- WHAT A SCREEN HOLDS

  enum Level:
    case Info, Warning, Problem

  /** what a screen holds: text, a notice, details, a form, a table, a section of these */
  sealed trait Part
  object Part:
    final case class Text(s: String) extends Part
    final case class Notice(level: Level, s: String) extends Part
    /** a record shown: label and value */
    final case class Details(rows: List[(String, Cell)]) extends Part
    final case class Section(title: String, parts: List[Part]) extends Part
    def text(s: String): Part = Text(s)
    def info(s: String): Part = Notice(Level.Info, s)
    def warning(s: String): Part = Notice(Level.Warning, s)
    def problem(s: String): Part = Notice(Level.Problem, s)
    /** label and value: text as it is, or a cell (a flag, money, a day) — no conversion asked of the caller */
    def details(rows: (String, String | Cell)*): Part = Details(rows.toList.map((label, v) => label -> (v match
      case s: String => Cell.Text(s)
      case c: Cell => c)))
    def section(title: String)(parts: Part*): Part = Section(title, parts.toList)
    /** a page elsewhere, by its address: a source to check, a document — never a button */
    final case class Link(label: String, url: String) extends Part
    def link(label: String, url: String): Part = Link(label, url)

  /**
   * A FORM: fields read together into a `P` — `Form(name, amount, due)` is a `Form[(String, BigDecimal, Day)]`,
   * `.as[Order]` an `Order`. An input of the screen it is on: a button that submits it names it.
   */
  final class Form[P] private[model] (val fields: List[Field[?]], build: List[Any] => P, values: P => List[Any],
    checks: List[(String, P => Boolean)]) extends Part:
    /** read into a case class of these fields' types, in order */
    def as[C](using m: Mirror.ProductOf[C] { type MirroredElemTypes = P & Tuple }): Form[C] =
      new Form[C](fields, vs => m.fromProduct(build(vs).asInstanceOf[Product]),
        c => c.asInstanceOf[Product].productIterator.toList,
        checks.map((msg, ok) => (msg, (c: C) => ok(build(c.asInstanceOf[Product].productIterator.toList)))))
    /** a rule across fields: `message` when `ok` fails */
    def check(message: String)(ok: P => Boolean): Form[P] = new Form(fields, build, values, checks :+ (message -> ok))
    /** starting with `p` */
    def filledWith(p: P): Form[P] =
      new Form(fields.zip(values(p)).map((f, v) => f.asInstanceOf[Field[Any]].initially(v)), build, values, checks)
    /** the entries read: the value, or the problems — of each field by its key (`Some`), of the form (`None`) */
    def read(entry: String => Entry): Either[List[(Option[String], String)], P] =
      val read = fields.map(f => f.key -> f.read(entry(f.key)))
      val wrong = read.collect { case (k, Left(m)) => (Some(k), m) }
      if wrong.nonEmpty then Left(wrong)
      else
        val p = build(read.map(_._2.toOption.get))
        checks.collect { case (msg, ok) if !ok(p) => (None, msg) } match
          case Nil => Right(p)
          case ps => Left(ps)

  object Form:
    type Values[T <: Tuple] <: Tuple = T match
      case EmptyTuple => EmptyTuple
      case Field[a] *: t => a *: Values[t]
    /** fields read together, into a tuple of their values */
    def apply[T <: Tuple](fields: T): Form[Values[T]] =
      val fs = fields.toList.asInstanceOf[List[Field[?]]]
      require(fs.map(_.key).distinct.size == fs.size, s"a form's keys are unique: ${fs.map(_.key).mkString(", ")}")
      new Form[Values[T]](fs, vs => Tuple.fromArray(vs.toArray[Any].map(_.asInstanceOf[Object])).asInstanceOf[Values[T]],
        p => p.asInstanceOf[Product].productIterator.toList, Nil)
    /** one field alone, its value as it is */
    def one[A](field: Field[A]): Form[A] = new Form[A](List(field), vs => vs.head.asInstanceOf[A], a => List(a), Nil)

  /** a value shown in a table or in details */
  enum Cell:
    case Blank
    case Text(s: String)
    case Number(n: BigDecimal)
    case Money(n: BigDecimal, currency: String)
    case Date(d: Day)
    case Flag(b: Boolean)

    def text: String = this match
      case Blank => ""
      case Text(s) => s
      case Number(n) => n.bigDecimal.toPlainString
      case Money(n, c) => s"${n.setScale(2).bigDecimal.toPlainString} $c"
      case Date(d) => d.toString
      case Flag(b) => if b then "yes" else "no"

  object Cell:
    given Conversion[String, Cell] = Text(_)

  final case class Column[R](title: String, cell: R => Cell)
  object Column:
    def text[R](title: String)(f: R => String): Column[R] = Column(title, r => Cell.Text(f(r)))
    def number[R](title: String)(f: R => BigDecimal): Column[R] = Column(title, r => Cell.Number(f(r)))
    def money[R](title: String, currency: String = "EUR")(f: R => BigDecimal): Column[R] =
      Column(title, r => Cell.Money(f(r), currency))
    def date[R](title: String)(f: R => Day): Column[R] = Column(title, r => Cell.Date(f(r)))
    def flag[R](title: String)(f: R => Boolean): Column[R] = Column(title, r => Cell.Flag(f(r)))

  /** A TABLE: rows, its columns, each row a key; an input of the screen: a button that needs a row selected names it */
  final class Table[R] private[model] (val rows: List[R], val columns: List[Column[R]], val key: R => String,
    val empty: String) extends Part:
    def keyedBy(f: R => String): Table[R] = new Table(rows, columns, f, empty)
    /** what is shown when there is no row */
    def whenEmpty(s: String): Table[R] = new Table(rows, columns, key, s)
    def row(k: String): Option[R] = rows.find(r => key(r) == k)
    def cells(r: R): List[Cell] = columns.map(_.cell(r))

  object Table:
    /** rows, keyed by their place unless `keyedBy` */
    def apply[R](rows: List[R])(columns: Column[R]*): Table[R] =
      val indexed = rows.zipWithIndex.toMap
      new Table[R](rows, columns.toList, r => (indexed(r) + 1).toString, "nothing here")

  // ---- BUTTONS AND SCREENS

  enum Style:
    case Primary, Plain, Danger

  /** what a button reads from its screen: a form's values, a table's selected row */
  trait Inputs:
    def form[P](f: Form[P]): Either[Problems, P]
    def selected[R](t: Table[R]): Either[Problems, R]

  /** A BUTTON: its label, its style, the inputs it needs, and what pressing it gives */
  final class Button[A](val label: String, val style: Style, val needs: List[Part],
    val make: Inputs => Either[Problems, A]):
    def styled(s: Style): Button[A] = new Button(label, s, needs, make)

  object Button:
    /** pressed, gives `a` */
    def apply[A](label: String, style: Style = Style.Plain)(a: => A): Button[A] =
      new Button(label, style, Nil, _ => Right(a))
    /** reads and checks `form`, gives `f` of its value */
    def submit[P, A](label: String, form: Form[P], style: Style = Style.Primary)(f: P => A): Button[A] =
      new Button(label, style, List(form), in => in.form(form).map(f))
    /** needs a row of `table` selected, gives `f` of it */
    def select[R, A](label: String, table: Table[R], style: Style = Style.Primary)(f: R => A): Button[A] =
      new Button(label, style, List(table), in => in.selected(table).map(f))

  /** what a toolkit sends back: the button pressed (its index), the entries by (input, field key) — a field not
   * there is as it started — and the rows selected by input; the inputs, the screen's forms and tables, numbered in
   * the order they are on it */
  final case class Answer(button: Int, entries: Map[(Int, String), Entry] = Map.empty,
    selected: Map[Int, String] = Map.empty)

  /** A SCREEN: its title, what it holds, its buttons; pressing one gives an `A` */
  final case class Screen[A](title: String, parts: List[Part], buttons: List[Button[A]]):
    /** its forms and tables, in order: a toolkit's inputs, numbered */
    val inputs: Vector[Part] =
      def walk(ps: List[Part]): List[Part] = ps.flatMap {
        case Part.Section(_, inner) => walk(inner)
        case f: Form[?] => List(f)
        case t: Table[?] => List(t)
        case _ => Nil
      }
      walk(parts).toVector
    /** the number of a form or table on this screen */
    def input(p: Part): Int = inputs.indexWhere(_ eq p)
    buttons.foreach(b => b.needs.foreach(p =>
      require(input(p) >= 0, s"the button '${b.label}' needs a form or table that is not on the screen '$title'")))

    /** the button pressed, read against what was entered: its result, or the problems */
    def press(answer: Answer): Either[Problems, A] =
      buttons.lift(answer.button) match
        case None => Left(List(Problem(Problem.At.Screen, s"no button ${answer.button} on '$title'")))
        case Some(b) =>
          b.make(new Inputs:
            def form[P](f: Form[P]): Either[Problems, P] =
              val i = input(f)
              // an entry not sent is the field as it started
              f.read(k => answer.entries.getOrElse((i, k), f.fields.find(_.key == k).fold(Entry.Blank)(_.initial)))
                .left.map(_.map {
                case (Some(k), m) => Problem(Problem.At.Field(i, k), m)
                case (None, m) => Problem(Problem.At.Screen, m)
              })
            def selected[R](t: Table[R]): Either[Problems, R] =
              val i = input(t)
              answer.selected.get(i).flatMap(t.row).toRight(List(Problem(Problem.At.Rows(i), "select a row"))))

  object Screen:
    def apply[A](title: String)(parts: Part*)(buttons: Button[A]*): Screen[A] =
      Screen[A](title, parts.toList, buttons.toList)

  // ---- THE EFFECT AND ITS STEPS

  /** THE EFFECT: a screen shown, answered by the result of the button pressed */
  enum Ui[X]:
    case Show[A](screen: Screen[A]) extends Ui[A]
    /** a screen shown WHILE THE PROGRAM GOES ON, answered at once: progress. No button: the next screen replaces it */
    case Working(screen: Screen[Nothing]) extends Ui[Unit]

  /** A PROGRAM OF `Ui`, IN STEPS — of no kernel: what every toolkit drives. `resume` runs the rest of the program up
   * to its next screen; resuming an older `Waiting` again goes back to it */
  enum Step[A]:
    case Done(a: A)
    case Waiting[A, S](screen: Screen[S], resume: S => Step[A]) extends Step[A]
    /** a screen shown while the program goes on; `next()` the rest, run when the toolkit has shown it */
    case Working(screen: Screen[Nothing], next: () => Step[A])

  extension [A, S](w: Step.Waiting[A, S])
    /** a toolkit's answer: the next step, or the problems to show on this one */
    def answer(a: Answer): Either[Problems, Step[A]] = w.screen.press(a).map(w.resume)

  // ---- A TOOLKIT FOR TESTS: a person by script

  /** what the person does: presses a button, after entering text in fields by label, and selecting a row by any of
   * its cells' text */
  enum Act:
    case Press(button: String, entries: List[(String, String)], row: Option[String])

  object Act:
    def press(button: String, entries: (String, String)*): Act = Press(button, entries.toList, None)
    def choose(button: String, row: String, entries: (String, String)*): Act = Press(button, entries.toList, Some(row))

  /** how a play went: the result if the program ended, the titles seen, the problems of the last act, the screen it
   * stopped at */
  final case class Played[A](result: Option[A], seen: List[String], problems: Problems, at: Option[Screen[?]])

  /** THE TEST TOOLKIT: a program played by a script of acts, as a person would, by labels */
  object Play:
    def apply[A](first: Step[A], acts: Act*): Played[A] =
      @annotation.tailrec
      def go(step: Step[A], acts: List[Act], seen: List[String]): Played[A] = step match
        case Step.Done(a) => Played(Some(a), seen.reverse, Nil, None)
        case Step.Working(screen, next) => go(next(), acts, screen.title :: seen)
        case w: Step.Waiting[A, s] =>
          val shown = w.screen.title :: seen
          acts match
            case Nil => Played(None, shown.reverse, Nil, Some(w.screen))
            case act :: rest => w.answer(answerOf(w.screen, act)) match
                case Left(ps) => Played(None, shown.reverse, ps, Some(w.screen))
                case Right(next) => go(next, rest, shown)
      go(first, acts.toList, Nil)

    private def answerOf(screen: Screen[?], act: Act): Answer = act match
      case Act.Press(label, entries, row) =>
        val button = screen.buttons.indexWhere(_.label == label)
        require(button >= 0, s"no button '$label' on '${screen.title}': ${screen.buttons.map(_.label).mkString(", ")}")
        val fields = for
          (p, i) <- screen.inputs.zipWithIndex.toList
          f <- p match { case f: Form[?] => f.fields; case _ => Nil }
        yield (i, f)
        val typed = entries.map { (label, text) =>
          val (i, f) = fields.find((_, f) => f.label == label || f.key == label)
            .getOrElse(throw IllegalArgumentException(s"no field '$label' on '${screen.title}'"))
          (i, f.key) -> Entry.Text(text)
        }.toMap
        val selected = row.toList.flatMap { text =>
          screen.inputs.zipWithIndex.collect { case (t: Table[r], i) =>
            t.rows.find(r => t.cells(r).exists(_.text == text)).map(r => i -> t.key(r))
          }.flatten
        }.toMap
        Answer(button, typed, selected)
