package okay.business

import okay.effects.{Effects, Runner}

/** ON AN IMPLEMENTATION OF OKAY-SIMPLE'S INTERFACE, `fx`: its types are the adapter's */
class OnSimple[F <: Runner & Singleton](val fx: F) extends Business:
  infix type ![+A, R[_]] = fx.![A, R]
  infix type +[R[_], E[_]] = [X] =>> okay.effects.Sum[R, E, X]
  type Pure[X] = fx.Pure[X]
  type In[E[_], R[_]] = fx.In[E, R]
  type Split[E[_], R[_], Rest[_]] = fx.Split[E, R, Rest]

  def only[E[_]]: Split[E, E, Pure] = fx.splitOne[E]
  def pure[A, R[_]](a: A): A ! R = fx.pure(a)
  def flatMap[A, B, R[_]](p: A ! R)(f: A => B ! R): B ! R = fx.flatMap[A, B, R](p)(f)
  def map[A, B, R[_]](p: A ! R)(f: A => B): B ! R = fx.map[A, B, R](p)(f)
  def defer[A, R[_]](p: => A ! R): A ! R = fx.delay(p)
  def performAt[E[_], R[_], X](op: E[X])(using in: In[E, R]): X ! R = fx.perform[E].apply[R, X](op)
  def handle[E[_], R[_], Rest[_], A, B](p: A ! R, h: Handler[E, Rest, A, B])(using s: Split[E, R, Rest]): B ! Rest =
    h match
      case Handler.General(onValue, onOp) =>
        fx.handle[E, R, Rest, A, B](p, new fx.Handler[E, Rest, A, B]:
          def ret(a: A): B ! Rest = onValue(a)
          def op[X](e: E[X], k: X => B ! Rest): B ! Rest = onOp[X](e, k))
      case Handler.Tail(onValue, onOp) =>
        fx.handle[E, R, Rest, A, B](p, new fx.TailHandler[E, Rest, A, B]:
          def ret(a: A): B ! Rest = onValue(a)
          def answer[X](e: E[X]): X = onOp[X](e))
  def run[A](p: A ! Pure): A = fx.run(p)

/** ON OKAY-SIMPLE'S MACHINE: `import okay.business.simple.*` */
object simple extends OnSimple[okay.effects.machine.Machine.type](okay.effects.machine.Machine):
  export okay.effects.machine.Machine.{splitOne, splitLast, splitInit}

/** ON OKAY-SIMPLE'S FREE MONAD: `import okay.business.free.*` */
object free extends OnSimple[okay.effects.free.Free.type](okay.effects.free.Free):
  export okay.effects.free.Free.{splitOne, splitLast, splitInit}
