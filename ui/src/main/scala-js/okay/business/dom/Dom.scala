package okay.business.dom

import okay.business.faces.{OnFaces, Skin}
import okay.business.laminar.{Later, Page}
import okay.business.model.{Screen, Step, Ui}
import okay.effects.cek.OnCek
import okay.faces.Face
import okay.faces.dom.DomFace
import okay.scala3.*
import org.scalajs.dom

/**
 * `Ui` IN A PAGE BY OKAY-FACES' DOM FACE (specs/business.md, live screens): the handler of `Ui` into `Async`, as
 * `Laminar.run` is — `Async.toFuture(Dom.run(root)(Fetch.run(App.main)))`; a row of `Lifted[Future]` still taken,
 * deprecated (`Later`) — but the screens of the program are ONE face:
 * a screen that follows another with the same buttons changes the page in place, so a screen shown again on every
 * event (`screen.until(events)`) keeps what was typed, the focus, the scroll. The page wears `skin`; `follow`: a screen
 * that grows keeps the page's end in view when a person was at it — a conversation, a log.
 */
object Dom:
  def run[R[_], Rest[_], A](root: dom.Element, skin: Skin = Skin.default, follow: Boolean = false)(p: A ! R)(
    using at: Split[Ui, R, Rest], later: Later[Rest]): A ! Rest =
    defer {
      Page.install(skin)
      val face = new DomFace[OnCek.type]()(OnCek)
      val session = face.session[Any](DomFace.Settings(look = Some(skin.look), follow = follow))
      var mounted = false
      def draw[S](screen: Screen[S], answered: S => Unit): Unit =
        if !mounted then
          val frame = dom.document.createElement("div")
          frame.setAttribute("class", "okay-ui")
          // the look's own stylesheet first, as okay-faces' faces put it: the skin's rules go over it
          frame.appendChild(face.world(face.stylesheet(skin.look)))
          frame.appendChild(session.element)
          root.appendChild(frame)
          mounted = true
        val program = OnFaces.program(Step.Waiting[S, S](screen, s => Step.Done(s)))
          .asInstanceOf[okay.effects.tagless.Program[Any, Face]]
        session.show(face.faces.steps[Any, Face, OnCek.Pure](face.faces.of(program)), a => answered(a.asInstanceOf[S]))
      handle(p, Handler.general[Ui, Rest, A, A](pure)([X] => (op: Ui[X], k: X => A ! Rest) => op match
        case Ui.Show(screen) =>
          later[X](answer => draw[X](screen, answer)).flatMap(k)
        case Ui.Working(screen) =>
          draw[Nothing](screen, _ => ())
          k(())
        // a point passed: a page keeps nothing of it
        case Ui.Mark(_, _) => k(())))
    }
