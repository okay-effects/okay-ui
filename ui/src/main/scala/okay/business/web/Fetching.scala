package okay.business.web

import okay.http.{Fetch, Request, Response}
import okay.scala3.*
import okay.scala3.std.Async
import scala.concurrent.{ExecutionContext, Future}

/**
 * OKAY-HTTP'S CLIENT EFFECT ON OKAY-SCALA: a program performs okay-http's `Fetch` (`Fetch.Call`, `Fetch.Send`) and
 * this handler sends it by a transport — the full URL and the request to a `Future[Response]` — its answer awaited
 * through okay-scala's `Async`, as a page needs. The transports: okay-http's `Browser.fetch` in a page
 * (`InThePage.fetching`), the JDK's client on the JVM (`OnTheJvm.fetching`), any function in a test.
 */
object Fetching:
  type Transport = (String, Request) => Future[Response]

  def by[R[_], Rest[_], A](send: Transport)(p: A ! R)(
    using Split[Fetch, R, Rest], In[Async, Rest], ExecutionContext): A ! Rest =
    handle(p, Handler.general[Fetch, Rest, A, A](a => pure(a))([X] => (op: Fetch[X], k: X => A ! Rest) => op match
      case Fetch.Call(method, url, headers, body, answer) =>
        Async.fromFuture[Response, Rest](send(url, Fetch.request(method, url, headers, body))).flatMap(r => k(answer(r)))
      case Fetch.Send(base, request, answer) =>
        Async.fromFuture[Response, Rest](send(Fetch.target(base, request), request)).flatMap(r => k(answer(r)))))
