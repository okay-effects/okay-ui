package okay.business.web

import okay.business.scala3.*
import okay.http.{Fetch, Request, Response}
import okay.scala3.std.Async
import scala.concurrent.{ExecutionContext, Future}

/** Settings kept, and okay-http's `Fetch` sent by a transport through okay-scala's `Async` */
class WorldSuite extends munit.FunSuite:
  given ExecutionContext = munitExecutionContext

  test("a setting kept is read back; none is None"):
    type App[X] = Settings[X]
    val kept = scala.collection.mutable.Map("a" -> "1")
    val p = setting[App]("a").flatMap(a => keep[App]("b", "2").andThen(setting[App]("c").map(c => (a, c))))
    assertEquals(run(keptIn(kept)(p)), (Some("1"), None))
    assertEquals(kept.get("b"), Some("2"))

  test("a Call and a Send reach the transport with their full URLs; the answers come back"):
    type App[X] = (Async + Fetch)[X]
    var asked = List.empty[(String, String)]
    val transport: Fetching.Transport = (url, r) =>
      asked :+= (url, r.method)
      Future.successful(Response(200, Map.empty, s"answer to $url"))
    val p = perform[Fetch][App, Response](Fetch.Call("GET", "https://example.org/a?b=1", Map("k" -> "v"), "", r => r))
      .flatMap(a => perform[Fetch][App, Response](Fetch.Send("https://x.org", Request("POST", "/y"), r => r))
        .map(b => (a.body, b.body)))
    Async.toFuture(Fetching.by(transport)(p)).map { got =>
      assertEquals(got, ("answer to https://example.org/a?b=1", "answer to https://x.org/y"))
      assertEquals(asked, List("https://example.org/a?b=1" -> "GET", "https://x.org/y" -> "POST"))
    }
