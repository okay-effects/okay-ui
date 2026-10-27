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

/** each found by the effect's `Place`, not its `In`: a missing `In` is a compile error at once (okay-scala's message),
 * where a missing `Place` lets the search try the next */
object Later extends LaterByFuture:
  given async[R[_]](using at: Place[Async, R, ?]): Later[R] with
    def apply[X](register: (X => Unit) => Unit): X ! R =
      given In[Async, R] = In(at)
      Async.async[X, R] { cb =>
        var answered = false
        register(x => if !answered then { answered = true; cb(Right(x)) })
      }

trait LaterByFuture:
  /** DEPRECATED: a row of `Lifted[Future]`, lowered at the top by `Lift.lower` — a row of `Async` instead */
  given future[R[_]](using at: Place[Lifted[Future], R, ?]): Later[R] with
    def apply[X](register: (X => Unit) => Unit): X ! R =
      val pressed = Promise[X]()
      register(x => { val _ = pressed.trySuccess(x) })
      Lift.lift[Future, X, R](pressed.future)(using In(at))
