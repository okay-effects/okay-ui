package okay.business.faces

import okay.business.model.*
import okay.effects.shape.Shape
import okay.effects.tagless.Program
import okay.faces.{Action, Face, Meanwhile, View}
import okay.faces.splitOne

/**
 * THE BUSINESS SCREENS ON OKAY-FACES: a program in steps (`Step`, of any kernel) as a program of okay-faces' `Face`,
 * so every face of okay-faces runs it — a terminal, a page, a window, an agent's tools, a test script. A screen is a
 * view and its buttons actions; a button's forms and table its action's parameters, each field a `Shape` that reads
 * the entry the business field's way, so a field's problem is shown at the field and the step waits again. A problem
 * the screen finds beyond its fields (a rule across them) is shown as a notice, and the screen offered again.
 */
object OnFaces:
  /** the program, as okay-faces runs it: `Terminal.console(OnFaces.program(app))` */
  def program[A](first: Step[A]): Program[A, Face] = first match
    case Step.Done(a) => okay.faces.pure[A, Face](a)
    case w: Step.Waiting[A, s] => waiting(w, Nil)
    // shown, and on at once: a face tells it and runs the rest
    case Step.Working(screen, next) =>
      okay.faces.flatMap(okay.faces.show[Face](view(screen, Nil)))(_ => program(next()))

  private def waiting[A, S](w: Step.Waiting[A, S], problems: Problems): Program[A, Face] =
    def answered(answer: Answer): Program[A, Face] = w.answer(answer) match
      case Left(ps) => waiting(w, ps)
      case Right(next) => program(next)
    w.screen.events match
      case None =>
        okay.faces.flatMap(okay.faces.offer[Answer, Face](view(w.screen, problems), actions(w.screen)))(answered)
      // AN EVENT MEANWHILE answers the screen as a button would: the step's `Meanwhile`
      case Some(events) =>
        val pressed: List[Action[Either[S, Answer]]] = actions(w.screen).map(a =>
          Action.of[a.P, Either[S, Answer]](a.name, a.label, a.params, a.doc, a.more, a.long)(p => Right(a.make(p))))
        val meanwhile = new Meanwhile[Either[S, Answer]](k => events.register(s => k(Left(s))))
        okay.faces.flatMap(okay.faces.offer[Either[S, Answer], Face](view(w.screen, problems), pressed, meanwhile)) {
          case Left(s) => program(w.resume(s))
          case Right(answer) => answered(answer)
        }

  // ---- WHAT IS SHOWN

  def view(screen: Screen[?], problems: Problems): View =
    val at = placed(screen)
    View.stack((View.heading(screen.title) :: problems.map(p => View.problem(p.message)) :::
      screen.parts.flatMap(part(_, at)))*)

  /** THE BUTTONS AT A SLOT: those that need no input and whose slot is on the screen — each with its key, a ref's */
  def placed(screen: Screen[?]): Map[String, List[(Button[?], String, Int)]] =
    def slots(ps: List[Part]): List[String] = ps.flatMap {
      case Part.Slot(n) => List(n)
      case Part.Section(_, inner) => slots(inner)
      case Part.Block(_, _, inner) => slots(inner)
      case _ => Nil
    }
    val there = slots(screen.parts).toSet
    val at = screen.buttons.zipWithIndex.collect {
      case (b, i) if b.needs.isEmpty && b.slot.exists(there) => (b, s"${b.slot.get}/${Action.nameOf(b.label)}", i)
    }
    // a key twice: its place added
    val keys = at.map(_._2)
    at.zipWithIndex.map { case ((b, k, i), j) => (b, if keys.indexOf(k) == j then k else s"$k-$i", i) }
      .groupBy(_._1.slot.get)

  private def part(p: Part, at: Map[String, List[(Button[?], String, Int)]]): List[View] = p match
    case Part.Text(s) => List(View.text(s))
    case Part.Notice(Level.Info, s) => List(View.info(s))
    case Part.Notice(Level.Warning, s) => List(View.warning(s))
    case Part.Notice(Level.Problem, s) => List(View.problem(s))
    case Part.Details(rows) => List(View.Fields(rows.map((l, c) => l -> View.text(c.text)).toVector))
    case Part.Section(title, parts) => List(View.Group(Some(title), parts.flatMap(part(_, at)).toVector))
    case Part.Block(mark, title, parts) =>
      List(View.Marked(mark, View.Group(title, parts.flatMap(part(_, at)).toVector)))
    case Part.Slot(name) =>
      val buttons = at.getOrElse(name, Nil).map((b, key, _) =>
        View.Marked(s"button-${b.style.toString.toLowerCase}", View.Marked("button", View.Ref(key, b.label))))
      if buttons.isEmpty then Nil else List(View.Marked("slot", View.Group(None, buttons.toVector)))
    case Part.Link(label, url) => List(View.Link(label, url))
    case t: Table[r] =>
      if t.rows.isEmpty then List(View.text(t.empty))
      else List(View.Table(t.columns.map(_.title).toVector, t.rows.map(r => t.cells(r).map(c => View.text(c.text))
        .toVector).toVector))
    // a form is its button's parameters: shown where the face asks for them
    case _: Form[?] => Nil

  // ---- WHAT CAN BE DONE

  def actions(screen: Screen[?]): List[Action[Answer]] =
    val at = placed(screen).values.flatten.toList
    val atIndex = at.map(_._3).toSet
    val names = screen.buttons.map(b => Action.nameOf(b.label))
    val unique = names.zipWithIndex.map((n, i) => if names.indexOf(n) == i then n else s"$n-${i + 1}")
    val bar = screen.buttons.zipWithIndex.zip(unique).filterNot { case ((_, i), _) => atIndex(i) }.map {
      case ((b, i), name) =>
        if b.needs.isEmpty then Action.plain[Answer](name, b.label)(Answer(i))
        else Action[Answer, Answer](name, b.label, params(screen, i, b))(a => a).withMore(held(screen, b)*)
          .withLong(long(screen, b)*)
    }
    // the buttons at slots: refs, chosen by their keys
    val keys = at.map((_, key, i) => key -> i).toMap
    // the key a text, checked here: the action the same however many buttons stand at slots — so a face may keep the
    // page when a screen follows with more (okay-faces: the same actions, the view changed in place)
    val key = Shape.Field[String]("key", Shape.text.refine(k =>
      if keys.contains(k) then Right(k) else Left(s"no button $k here"))(identity), Some("Button"))
    val ref = Option.when(at.nonEmpty)(Action[String, Answer]("ref", "Press",
      Shape.fields[String](List(key))(vs => vs.head.asInstanceOf[String])(k => List(k)))(k => Answer(keys(k))))
    bar ++ ref.toList

  /** the parameters of a button that are long text (`Kind.LongText`): text areas where a face can */
  private def long(screen: Screen[?], b: Button[?]): List[String] =
    val several = b.needs.size > 1
    b.needs.flatMap {
      case f: Form[?] =>
        val i = screen.input(f)
        f.fields.filter(_.kind == Kind.LongText).map(field => if several then s"$i-${field.key}" else field.key)
      case _ => Nil
    }

  /** the parameters of a button its forms hold back (`Field.more`): drawn under "Options" where a face can */
  private def held(screen: Screen[?], b: Button[?]): List[String] =
    val several = b.needs.size > 1
    b.needs.flatMap {
      case f: Form[?] =>
        val i = screen.input(f)
        f.fields.filter(_.held).map(field => if several then s"$i-${field.key}" else field.key)
      case _ => Nil
    }

  /** a button's parameters: its forms' fields and its tables' rows, read into its `Answer` */
  private def params(screen: Screen[?], button: Int, b: Button[?]): Shape[Answer] =
    val several = b.needs.size > 1
    // each parameter: its shape's field, where its value goes in the answer, and where it is in one
    final case class Param(field: Shape.Field[?], put: (Answer, Any) => Answer, get: Answer => Any)
    val ps: List[Param] = b.needs.flatMap { p =>
      val i = screen.input(p)
      p match
        case f: Form[?] => f.fields.map { field =>
            val name = if several then s"$i-${field.key}" else field.key
            Param(shapeOf(field, name), (a, v) => a.copy(entries = a.entries + ((i, field.key) -> v.asInstanceOf[Entry])),
              a => a.entries.getOrElse((i, field.key), field.initial))
          }
        case t: Table[r] =>
          val texts = t.rows.map(r => t.cells(r).map(_.text).filter(_.nonEmpty).mkString(" | "))
          val labels = texts.zipWithIndex.map((s, j) => if texts.indexOf(s) == j then s else s"$s (${j + 1})")
          val name = if several then s"$i-row" else "row"
          List(Param(Shape.Field[String](name, Shape.choice(labels.zip(t.rows.map(t.key))*), Some("Row")),
            (a, v) => a.copy(selected = a.selected + (i -> v.asInstanceOf[String])),
            a => a.selected.getOrElse(i, "")))
        case _ => Nil
    }
    Shape.fields[Answer](ps.map(_.field))(vs => ps.zip(vs).foldLeft(Answer(button)) { case (a, (p, v)) => p.put(a, v) })(
      a => ps.map(_.get(a)))

  /** a business field as a shape: its widget by its kind, read the field's way — required, rules and all */
  def shapeOf[A](field: Field[A], name: String): Shape.Field[Entry] =
    val checked: Entry => Either[String, Entry] = e => field.read(e).map(_ => e)
    val base: Shape[Entry] = field.kind match
      case Kind.Text | Kind.LongText | Kind.Secret => Shape.text.refine(s => checked(Entry.Text(s)))(_.text)
      case Kind.Whole | Kind.Decimal | Kind.Money(_) =>
        Shape.number.refine(n => checked(Entry.Number(n))) {
          case Entry.Number(n) => n
          case e => scala.util.Try(BigDecimal(e.text)).getOrElse(BigDecimal(0))
        }
      case Kind.Date =>
        Shape.date.refine(d => checked(Entry.Date(Day(d.year, d.month, d.day)))) {
          case Entry.Date(d) => okay.effects.shape.Day(d.year, d.month, d.day)
          case e => Day.parse(e.text).fold(_ => okay.effects.shape.Day(1970, 1, 1),
              d => okay.effects.shape.Day(d.year, d.month, d.day))
        }
      case Kind.Flag => Shape.bool.refine(b => checked(Entry.Flag(b))) {
          case Entry.Flag(b) => b
          case _ => false
        }
      case Kind.Choice(options) =>
        Shape.choice(options.map((k, l) => (l, k))*).refine(k => checked(Entry.Pick(k)))(_.text)
    val shape =
      if field.required then base
      else base.optional.map(_.getOrElse(Entry.Blank))(e => if e.isBlank then None else Some(e))
    Shape.Field[Entry](name, shape, Some(field.label), field.hint,
      secret = field.kind == Kind.Secret, default = Option(field.initial).filterNot(_.isBlank))
