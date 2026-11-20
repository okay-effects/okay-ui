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

  /** `Address` in the page's URL, its fragment (`#/ru/pesel/who=ua`, never sent to a server): opened where the URL
   * says; each new place a new entry of the browser's history, so a link to the screen can be sent and Back goes to
   * the screen before — with `onBack` starting the program again from there */
  def address[R[_], Rest[_], A](p: A ! R)(using Split[okay.business.model.Address, R, Rest]): A ! Rest =
    import okay.business.model.Address
    handle(p, Handler.tail[Address, Rest, A, A](a => pure(a))([X] => (asked: Address[X]) => asked match
      case Address.Opened() =>
        Address.path(scalajs.js.URIUtils.decodeURI(dom.window.location.hash))
      case Address.At(path) =>
        val fragment = Address.fragment(path.map(scalajs.js.URIUtils.encodeURI))
        if dom.window.location.hash != fragment then
          val here = dom.window.location.pathname + dom.window.location.search
          dom.window.history.pushState(null, "", if fragment.isEmpty then here else fragment)))

  /** `again` when the person goes Back or Forward, or opens a link of this page in it: the program, run again, is
   * opened where the URL now says (`address`) */
  def onBack(again: () => Unit): Unit =
    dom.window.addEventListener("popstate", (_: dom.Event) => again())

  /** `Settings` in the page's local storage, each name under `prefix` */
  def kept[R[_], Rest[_], A](prefix: String)(p: A ! R)(using Split[Settings, R, Rest]): A ! Rest =
    handle(p, Handler.tail[Settings, Rest, A, A](a => pure(a))([X] => (asked: Settings[X]) => asked match
      case Settings.Get(n) => Option(dom.window.localStorage.getItem(prefix + n))
      case Settings.Put(n, v) => dom.window.localStorage.setItem(prefix + n, v)))
