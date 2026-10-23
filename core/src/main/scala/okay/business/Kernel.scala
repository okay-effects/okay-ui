package okay.business

import scala.annotation.targetName

/**
 * THE KERNEL'S WORDS: the least a business application needs of an implementation of effects — the rows `!`, `+`,
 * `%`, the evidence `In`, `Split`, `Has`, the words `pure`, `flatMap`, `map`, `perform`, `handle`, `run`. Every type
 * is the implementation's own: an adapter (`okay.business.scala3`, `okay.business.simple`) is an object of this trait
 * whose types ARE its implementation's, so an application switches kernels by its import alone.
 */
trait Kernel:
  /** a program of the value `A` over the row `R` */
  infix type ![+A, R[_]]
  /** the row `R` and the effect `E` after it */
  infix type +[R[_], E[_]] <: [X] =>> Any
  /** the effect `F` with its parameter `S`: `State % Int` */
  infix type %[F[_, _], S] = [X] =>> F[S, X]
  /** the row of no effect */
  type Pure[X]
  /** `E` is in `R` */
  type In[E[_], R[_]]
  /** `E` is in `R`, and `R` without it is `Rest` */
  type Split[E[_], R[_], Rest[_]] <: In[E, R]
  /** a context bound: `def order[R[_]: Has[Ui]]: Order ! R` */
  type Has[E[_]] = [R[_]] =>> In[E, R]

  /** `E` alone is a row: what a handler of a program of one effect takes */
  def only[E[_]]: Split[E, E, Pure]

  def pure[A, R[_]](a: A): A ! R
  def flatMap[A, B, R[_]](p: A ! R)(f: A => B ! R): B ! R
  def map[A, B, R[_]](p: A ! R)(f: A => B): B ! R
  /** the program made when the run gets to it: a loop in constant stack */
  def defer[A, R[_]](p: => A ! R): A ! R
  /** an operation of `E` performed, at its place in `R` */
  def performAt[E[_], R[_], X](op: E[X])(using In[E, R]): X ! R
  /** `E` handled by `h` from wherever it is in `p`'s row */
  def handle[E[_], R[_], Rest[_], A, B](p: A ! R, h: Handler[E, Rest, A, B])(using Split[E, R, Rest]): B ! Rest
  /** a program of no effect, run */
  def run[A](p: A ! Pure): A

  /** `perform[Ui](Ui.Show(screen))`: the effect said, the answer from the operation, the row from what is expected */
  final def perform[E[_]]: Performing[E] = Performing[E]()
  final class Performing[E[_]]:
    def apply[R[_], X](op: E[X])(using In[E, R]): X ! R = performAt[E, R, X](op)

  extension [A, R[_]](p: A ! R)
    @targetName("programFlatMap") def flatMap[B](f: A => B ! R): B ! R = Kernel.this.flatMap(p)(f)
    @targetName("programMap") def map[B](f: A => B): B ! R = Kernel.this.map(p)(f)
    def as[B](b: B): B ! R = Kernel.this.map(p)(_ => b)
    def andThen[B](q: => B ! R): B ! R = Kernel.this.flatMap(p)(_ => q)

  /** A HANDLER OF `E`, the row outside `R`, the body's value `A`, its own `B`, in one of two shapes */
  enum Handler[E[_], R[_], A, B]:
    /** `k` the rest of the body, under this handler: resumed once, many times, or never */
    case General(ret: A => B ! R, op: [X] => (E[X], X => B ! R) => B ! R)
    /** an operation answered in place: the body goes on at once */
    case Tail(ret: A => B ! R, answer: [X] => E[X] => X)
