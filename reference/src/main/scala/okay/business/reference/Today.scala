package okay.business.reference

import okay.business.scala3.*

/** THE DATE, as an effect: the program asks, it never reads a clock */
enum Today[X]:
  case Now() extends Today[Day]

/** a day fixed here; the system's clock on each platform (`TodayOnPlatform`) */
object Today extends TodayOnPlatform:
  def fixed(day: Day): Handles[Today] = Clock(() => day)

/** TODAY BY A CLOCK: the one handler of `Today` — the clock the platform's, or a test's that never moves */
final class Clock(now: () => Day) extends Handles[Today]:
  def apply[R[_], Rest[_], A](p: A ! R)(using Split[Today, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Today, Rest, A, A](a => pure(a), [X] => (asked: Today[X]) => asked match
      case Today.Now() => now()))
