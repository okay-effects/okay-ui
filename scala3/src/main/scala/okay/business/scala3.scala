package okay.business

import okay.scala3 as k

/** ON OKAY-SCALA'S MACHINE: `import okay.business.scala3.*` — the types are okay-scala's own */
object scala3 extends Business:
  infix type ![+A, R[_]] = k.![A, R]
  infix type +[R[_], E[_]] = [X] =>> k.Sum[R, E, X]
  type Pure[X] = k.Pure[X]
  type In[E[_], R[_]] = k.In[E, R]
  type Split[E[_], R[_], Rest[_]] = k.Split[E, R, Rest]

  def only[E[_]]: Split[E, E, Pure] = summon[k.Split[E, E, k.Pure]]
  def pure[A, R[_]](a: A): A ! R = k.pure(a)
  def flatMap[A, B, R[_]](p: A ! R)(f: A => B ! R): B ! R = k.Prog.FlatMap(p, f)
  def map[A, B, R[_]](p: A ! R)(f: A => B): B ! R = k.Prog.Map(p, f)
  def defer[A, R[_]](p: => A ! R): A ! R = k.defer(p)
  def performAt[E[_], R[_], X](op: E[X])(using in: In[E, R]): X ! R = k.perform[E](op)
  def handle[E[_], R[_], Rest[_], A, B](p: A ! R, h: Handler[E, Rest, A, B])(using s: Split[E, R, Rest]): B ! Rest =
    k.handle(p, h match
      case Handler.General(ret, op) => k.Handler.General[E, Rest, A, B](ret, op)
      case Handler.Tail(ret, answer) => k.Handler.Tail[E, Rest, A, B](ret, answer))
  def run[A](p: A ! Pure): A = k.run(p)
