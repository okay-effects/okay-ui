package okay.business.laminar

import okay.business.model.{Exiting, Screen, Ui}
import okay.scala3.*

/**
 * THE PAGE'S HANDLER OF `Ui`, for `Laminar.run` and `Dom.run`, each giving its own `draw` (specs/business.md, level
 * 6). `Show` draws its screen and waits for a press (`Later`); `Working` draws its screen and goes on at once — in an
 * interruptible block with the block's exits, a press of one leaving the block: the wait the program is in cancelled,
 * the program going on after the block (`Later.interruptible`). Only the screen drawn last takes a press.
 */
object PageUi:
  /** a toolkit's drawing of a screen into the page, `answered` called with what a press gives */
  type Draw = [S] => (Screen[S], S => Unit) => Unit

  def handled[R[_], Rest[_], A](p: A ! R, draw: Draw)(using at: Split[Ui, R, Rest], later: Later[Rest]): A ! Rest =
    defer {
      val exits = Exits[A, Rest]()
      var blocks: List[Exiting[A ! Rest]] = Nil
      // each screen drawn its number: a press on one drawn before the last is not taken
      var drawn = 0
      def show[S](screen: Screen[S], answered: S => Unit): Int =
        drawn += 1
        exits.offered = false
        draw[S](screen, answered)
        drawn
      later.interruptible(handle(p, Handler.general[Ui, Rest, A, A](pure)(
        [X] => (op: Ui[X], k: X => A ! Rest) => op match
          case Ui.Show(screen) =>
            later[X](answer => { val _ = show[X](screen, answer) }).flatMap(k)
          case Ui.Working(screen) =>
            if blocks.isEmpty || !later.interrupts then { val _ = show[Nothing](screen, _ => ()) }
            else
              val (buttons, leave) = Exiting.offered(blocks)
              var mine = 0
              mine = show[Int](Screen[Int](screen.title, screen.parts, buttons),
                i => if drawn == mine then exits.press(() => leave(i)))
              exits.offered = true
            k(())
          case x: Ui.Exits[e] =>
            val outside = blocks
            blocks = Exiting[A ! Rest, e](x.exits, ev => { blocks = outside; k(Some(ev)) }) :: blocks
            k(None)
          case Ui.Settled() =>
            blocks = blocks.drop(1)
            // the block's exits gone from the screen still shown: a press there is not taken
            drawn += 1
            exits.offered = false
            k(())
          // a point passed: a page keeps nothing of it
          case Ui.Mark(_, _) => k(()))), exits)
    }

/** A PRESS OF AN EXIT, on its way to the wait the program is in: `waiting` the wait's own taking of it, set while the
 * program waits on a screen with exits; pressed between two waits, kept for the next */
final class Exits[A, R[_]]:
  var offered = false
  var waiting: Option[(() => A ! R) => Unit] = None
  private var early: Option[() => A ! R] = None
  def press(leave: () => A ! R): Unit = waiting match
    case Some(take) => take(leave)
    case None => early = Some(leave)
  def pressedEarly(): Option[() => A ! R] =
    val e = early
    early = None
    e
