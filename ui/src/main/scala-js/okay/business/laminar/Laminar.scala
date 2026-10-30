package okay.business.laminar

import com.raquo.laminar.api.L.*
import okay.business.faces.{OnFaces, Skin}
import okay.business.model.{Screen, Step, Ui}
import okay.effects.cek.OnCek
import okay.faces.laminar.LaminarFace
import okay.scala3.*
import org.scalajs.dom

/**
 * `Ui` IN A PAGE, on okay-scala's machine (specs/business.md, level 6): the handler of `Ui` into okay-scala's `Async`,
 * so a program whose world answers later — a `fetch`, a timer, the page's storage — and whose screens answer when the
 * person presses a button is ONE program of one async effect, driven at the top:
 * `Async.toFuture(Laminar.run(root)(Fetch.run(App.main)))`. A row of `Lifted[Future]` instead, lowered by
 * `Lift.lower`, is still taken (`Later`), deprecated.
 *
 * `Show(screen)` draws the screen into `root` by okay-faces' Laminar face — its forms, its problems at their fields,
 * all of it the face's — and is answered by the button pressed, a `Future`. `Working(screen)` draws its screen and
 * goes on at once: progress, replaced by the next screen. The page wears `skin`, `Skin.default` unless another is
 * said (`Page.install`).
 */
object Laminar:
  def run[R[_], Rest[_], A](root: dom.Element, skin: Skin = Skin.default, browserHistory: Boolean = false)(
    p: A ! R)(using at: Split[Ui, R, Rest], later: Later[Rest]): A ! Rest =
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
        FilePicking.attachSoon(root, screen)
        okay.business.laminar.TreeToggling.attachSoon(root)
      PageUi.handled(p, [S] => (screen: Screen[S], answered: S => Unit) => draw[S](screen, answered))
    }
