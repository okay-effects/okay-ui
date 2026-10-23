package okay.business.laminar

import com.raquo.laminar.api.L.*
import okay.business.faces.{OnFaces, Skin}
import okay.business.model.{Screen, Step, Ui}
import okay.effects.cek.OnCek
import okay.faces.laminar.LaminarFace
import okay.scala3.*
import org.scalajs.dom
import scala.concurrent.{Future, Promise}

/**
 * `Ui` IN A PAGE, on okay-scala's machine (specs/business.md, level 6): the handler of `Ui` into `Lifted[Future]`, so a
 * program whose world answers later — a `fetch`, the page's storage — and whose screens answer when the person presses
 * a button is ONE program of futures, lowered once at the top: `Lift.lower(Laminar.run(root)(Fetch.run(App.main)))`.
 *
 * `Show(screen)` draws the screen into `root` by okay-faces' Laminar face — its forms, its problems at their fields,
 * all of it the face's — and is answered by the button pressed, a `Future`. `Working(screen)` draws its screen and
 * goes on at once: progress, replaced by the next screen. The page wears `skin`, `Skin.default` unless another is
 * said (`Page.install`).
 */
object Laminar:
  def run[R[_], Rest[_], A](root: dom.Element, skin: Skin = Skin.default, browserHistory: Boolean = false)(
    p: A ! R)(using Split[Ui, R, Rest], In[Lifted[Future], Rest]): A ! Rest =
    defer {
      Page.install(skin)
      val settings = Page.settings(skin, browserHistory)
      // the page this run draws into: made when the run starts, the screen shown its one child
      val face = new LaminarFace[OnCek.type]()(OnCek)
      val shown = Var[HtmlElement](div())
      var mounted = false
      def draw[S](screen: Screen[S], answered: S => Unit): Unit =
        if !mounted then
          render(root, div(cls := "okay-ui", child <-- shown.signal))
          mounted = true
        shown.set(face.handle(OnFaces.program(Step.Waiting[S, S](screen, s => Step.Done(s))), settings, answered))
      handle(p, Handler.general[Ui, Rest, A, A](pure)([X] => (op: Ui[X], k: X => A ! Rest) => op match
        case Ui.Show(screen) =>
          val pressed = Promise[X]()
          draw[X](screen, x => pressed.success(x))
          Lift.lift[Future, X, Rest](pressed.future).flatMap(k)
        case Ui.Working(screen) =>
          draw[Nothing](screen, _ => ())
          k(())))
    }
