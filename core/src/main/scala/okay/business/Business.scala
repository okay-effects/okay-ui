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

  /** THE PROGRAM AT A POINT IT CAN BE TAKEN UP FROM, its state as JSON text: a host that loses its memory keeps it,
   * and starts the program there again (okay-faces' points, `Durable`) */
  def mark[R[_]](point: String, state: String)(using In[Ui, R]): Unit ! R = perform[Ui](Ui.Mark(point, state))

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
   * `Waiting`, so a toolkit may resume an older one again: back; a `Working` goes on when the toolkit runs `next` */
  def steps[A](p: A ! Ui): Step[A] =
    run(handle[Ui, Ui, Pure, A, Step[A]](p, Handler.General[Ui, Pure, A, Step[A]](a => pure(Step.Done(a)),
      [X] => (op: Ui[X], k: X => Step[A] ! Pure) => op match
        case Ui.Show(screen) => pure[Step[A], Pure](Step.Waiting(screen, (x: X) => run(k(x))))
        case Ui.Working(screen) => pure[Step[A], Pure](Step.Working(screen, () => run(k(()))))
        case Ui.Mark(point, state) => pure[Step[A], Pure](Step.Marked(point, state, () => run(k(()))))))(
      using only[Ui]))
