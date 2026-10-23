package okay.business.reference

import okay.business.scala3.*

/** A HANDLER, as this application names one: the effect `E` taken out of any program's row. Every implementation of
 * an effect here is one of these — and nothing else knows its world */
trait Handles[E[_]]:
  def apply[R[_], Rest[_], A](p: A ! R)(using Split[E, R, Rest]): A ! Rest
