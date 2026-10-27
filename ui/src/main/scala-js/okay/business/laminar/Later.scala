package okay.business.laminar

import okay.scala3.*
import okay.scala3.std.{Async, Lift, Lifted}
import scala.concurrent.{Future, Promise}

/**
 * HOW A PAGE WAITS for an answer that comes later — a button pressed — in the row `R` (specs/business.md, level 6):
 * by okay-scala's `Async` when the row has it, an effect driven by its own loop, the wait cancelled with its run;
 * else by a `Lifted[Future]` lifted, the way before, DEPRECATED, kept for one release. `register` is given the
 * answer's callback; the first answer is taken, any later one ignored.
 */
trait Later[R[_]]:
  def apply[X](register: (X => Unit) => Unit): X ! R
  /** whether a wait can be cancelled by a press: an exit shown */
  def interrupts: Boolean
  /** `p` with each of its waits raced with a press of an exit (`exits`): pressed, the wait cancelled and the program
   * the exit leads to run in its place */
  def interruptible[A](p: A ! R, exits: Exits[A, R]): A ! R

/** each found by the effect's `Place`, not its `In`: a missing `In` is a compile error at once (okay-scala's message),
 * where a missing `Place` lets the search try the next */
object Later extends LaterByFuture:
  given async[R[_]](using at: Place[Async, R, ?]): Later[R] with
    val interrupts = true
    /** ONE INTERCEPTION of `Async` around the program: a wait while a screen with exits is shown is raced with a
     * press — `register`'s own cancel undoing the wait (a `fetch` aborted) — and the program the exit leads to is run
     * in the interception's place, intercepted again: one deep however often a person leaves */
    def interruptible[A](p: A ! R, exits: Exits[A, R]): A ! R =
      given In[Async, R] = In(at)
      def watched(q: A ! R): A ! R = intercept(q, Handler.general[Async, R, A, A](a => pure(a))(
        [X] => (op: Async[X], k: X => A ! R) => op match
          case Async.Suspend(register) => exits.pressedEarly() match
            case Some(leave) => watched(leave())
            case None if !exits.offered => perform[Async](op).flatMap(k)
            case None =>
              Async.cancellable[Either[() => A ! R, X], R] { cb =>
                var done = false
                def once(r: Either[Throwable, Either[() => A ! R, X]]): Unit = if !done then
                  done = true
                  exits.waiting = None
                  cb(r)
                val undo = register(r => once(r.map(Right(_))))
                exits.waiting = Some(leave => { undo(); once(Right(Left(leave))) })
                () => { exits.waiting = None; undo() }
              }.flatMap {
                case Right(x) => k(x)
                case Left(leave) => watched(leave())
              }))
      watched(p)

    def apply[X](register: (X => Unit) => Unit): X ! R =
      given In[Async, R] = In(at)
      Async.async[X, R] { cb =>
        var answered = false
        register(x => if !answered then { answered = true; cb(Right(x)) })
      }

trait LaterByFuture:
  /** DEPRECATED: a row of `Lifted[Future]`, lowered at the top by `Lift.lower` — a row of `Async` instead */
  given future[R[_]](using at: Place[Lifted[Future], R, ?]): Later[R] with
    /** a future cannot be cancelled: no exit shown */
    val interrupts = false
    def interruptible[A](p: A ! R, exits: Exits[A, R]): A ! R = p
    def apply[X](register: (X => Unit) => Unit): X ! R =
      val pressed = Promise[X]()
      register(x => { val _ = pressed.trySuccess(x) })
      Lift.lift[Future, X, R](pressed.future)(using In(at))
