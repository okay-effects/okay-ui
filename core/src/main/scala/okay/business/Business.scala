package okay.business

/**
 * A KERNEL WITH THE BUSINESS WORDS: what `import okay.business.scala3.*` (or `.simple.*`, `.free.*`) gives — the
 * kernel's types and words, the model, the words of `Ui`, and its standard handler, `steps`. Written once, here, over
 * the abstract kernel; each adapter is an object of this trait.
 */
trait Business extends Kernel:
  export model.*

  /** the screen shown; the result of the button pressed */
  def show[A, R[_]](screen: Screen[A])(using In[Ui, R]): A ! R = perform[Ui](Ui.Show(screen))

  /** a screen shown while the program goes on — progress: answered at once, no button; the next screen replaces it */
  def working[R[_]](title: String)(parts: Part*)(using In[Ui, R]): Unit ! R =
    perform[Ui](Ui.Working(Screen[Nothing](title, parts.toList, Nil)))

  /** A WAY OUT OF WORK: while `block` runs, every working screen shows `exits` (buttons of no input); `Right` of its
   * value when it ends, `Left(e)` when the person presses an exit — the block left where it is (in a page its wait
   * cancelled), the program going on here (specs/business.md, level 3) */
  def interruptible[E, A, R[_]](exits: Button[E]*)(block: => A ! R)(using In[Ui, R]): Either[E, A] ! R =
    perform[Ui](Ui.Exits(exits.toList)).flatMap {
      case None => block.flatMap(a => perform[Ui](Ui.Settled()).map(_ => Right(a)))
      case Some(e) => pure(Left(e))
    }

  /** THE PROGRAM AT A POINT IT CAN BE TAKEN UP FROM, its state as JSON text: a host that loses its memory keeps it,
   * and starts the program there again (okay-faces' points, `Durable`) */
  def mark[R[_]](point: String, state: String)(using In[Ui, R]): Unit ! R = perform[Ui](Ui.Mark(point, state))

  /** a setting kept between visits (`Settings`), if any */
  def setting[R[_]](name: String)(using In[Settings, R]): Option[String] ! R = perform[Settings](Settings.Get(name))

  /** a setting kept for the next visit */
  def keep[R[_]](name: String, value: String)(using In[Settings, R]): Unit ! R =
    perform[Settings](Settings.Put(name, value))

  /** SETTINGS KEPT IN `kept`, a map of the caller's: memory — a test's, a session that need not last */
  def keptIn[R[_], Rest[_], A](kept: scala.collection.mutable.Map[String, String])(p: A ! R)(
    using Split[Settings, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Settings, Rest, A, A](a => pure(a), [X] => (asked: Settings[X]) => asked match
      case Settings.Get(n) => kept.get(n)
      case Settings.Put(n, v) => kept(n) = v))

  /** `text` copied to the clipboard, where the platform has one: whether it was */
  def copy[R[_]](text: String)(using In[Clipboard, R]): Boolean ! R = perform[Clipboard](Clipboard.Copy(text))

  /** THE CLIPBOARD AS A LIST of what was copied, newest last: a test's */
  def copiedTo[R[_], Rest[_], A](copied: scala.collection.mutable.Buffer[String])(p: A ! R)(
    using Split[Clipboard, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Clipboard, Rest, A, A](a => pure(a), [X] => (asked: Clipboard[X]) => asked match
      case Clipboard.Copy(t) => copied += t; true))

  /** a text, read and acknowledged */
  def inform[R[_]](title: String, text: String)(using In[Ui, R]): Unit ! R =
    show(Screen[Unit](title)(Part.Text(text))(Button("OK", Style.Primary)(())))

  /** yes or no */
  def confirm[R[_]](question: String, title: String = "Confirm")(using In[Ui, R]): Boolean ! R =
    show(Screen[Boolean](title)(Part.Text(question))(Button("Yes", Style.Primary)(true), Button("No")(false)))

  /** a form filled and saved, or cancelled */
  def edit[P, R[_]](title: String, form: Form[P], parts: Part*)(using In[Ui, R]): Option[P] ! R =
    show(Screen[Option[P]](title)((parts :+ form)*)(Button.submit("Save", form)(Some(_)), Button("Cancel")(None)))

  /** a row of the table chosen, or none */
  def pick[T, R[_]](title: String, table: Table[T], parts: Part*)(using In[Ui, R]): Option[T] ! R =
    show(Screen[Option[T]](title)((parts :+ table)*)(Button.select("Choose", table)(Some(_)), Button("Cancel")(None)))

  /** one of the options, each a button */
  def menu[A, R[_]](title: String, options: (String, A)*)(using In[Ui, R]): A ! R =
    show(Screen[A](title)()(options.map((label, a) => Button(label)(a))*))

  /** THE STANDARD HANDLER: a program of `Ui` in steps, of no kernel — what every toolkit drives. `k` kept in each
   * `Waiting`, so a toolkit may resume an older one again: back; a `Working` goes on when the toolkit runs `next`. The
   * interruptible blocks the program is in passed along, not kept: a step resumed again has the blocks of its time */
  def steps[A](p: A ! Ui): Step[A] =
    type Go = List[Exiting[Step[A]]] => Step[A]
    run(handle[Ui, Ui, Pure, A, Go](p, Handler.General[Ui, Pure, A, Go](a => pure(_ => Step.Done(a)),
      [X] => (op: Ui[X], k: X => Go ! Pure) => op match
        case Ui.Show(screen) => pure[Go, Pure](in => Step.Waiting(screen, (x: X) => run(k(x))(in)))
        case Ui.Working(screen) => pure[Go, Pure](in =>
          if in.isEmpty then Step.Working(screen, () => run(k(()))(in))
          else
            val (buttons, leave) = Exiting.offered(in)
            Step.Leavable(Screen[Int](screen.title, screen.parts, buttons), () => run(k(()))(in), leave))
        case Ui.Mark(point, state) => pure[Go, Pure](in => Step.Marked(point, state, () => run(k(()))(in)))
        case x: Ui.Exits[e] => pure[Go, Pure](in =>
          run(k(None))(Exiting[Step[A], e](x.exits, ev => run(k(Some(ev)))(in)) :: in))
        case Ui.Settled() => pure[Go, Pure](in => run(k(()))(in.drop(1)))))(
      using only[Ui]))(Nil)
