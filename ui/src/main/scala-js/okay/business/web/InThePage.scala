package okay.business.web

import okay.business.model.Settings
import okay.http.Browser
import okay.scala3.*
import okay.scala3.std.Async
import org.scalajs.dom
import scala.concurrent.ExecutionContext

/** A PAGE'S WORLD: okay-http's `Fetch` by the browser's `fetch`; `Settings` in the page's local storage */
object InThePage:
  /** `Fetch` by the browser's `fetch` (okay-http's `Browser.fetch`): headers, a body, redirects followed */
  def fetching[R[_], Rest[_], A](p: A ! R)(using Split[okay.http.Fetch, R, Rest], In[Async, Rest],
    ExecutionContext): A ! Rest =
    Fetching.by((url, r) => Browser.fetch("")(summon[ExecutionContext])(r.copy(path = url, query = Map.empty)))(p)

  /** `Clipboard` by the browser's (`navigator.clipboard`): `false` where the page may not write it */
  def clipboard[R[_], Rest[_], A](p: A ! R)(using Split[okay.business.model.Clipboard, R, Rest]): A ! Rest =
    handle(p, Handler.tail[okay.business.model.Clipboard, Rest, A, A](a => pure(a))(
      [X] => (asked: okay.business.model.Clipboard[X]) => asked match
        case okay.business.model.Clipboard.Copy(t) =>
          val clip = dom.window.navigator.asInstanceOf[scalajs.js.Dynamic].clipboard
          if scalajs.js.isUndefined(clip) then false
          else
            clip.writeText(t)
            true))

  /** `Settings` in the page's local storage, each name under `prefix` */
  def kept[R[_], Rest[_], A](prefix: String)(p: A ! R)(using Split[Settings, R, Rest]): A ! Rest =
    handle(p, Handler.tail[Settings, Rest, A, A](a => pure(a))([X] => (asked: Settings[X]) => asked match
      case Settings.Get(n) => Option(dom.window.localStorage.getItem(prefix + n))
      case Settings.Put(n, v) => dom.window.localStorage.setItem(prefix + n, v)))
