package okay.business.web

import okay.http.{Bytes, Request, Response}
import okay.scala3.*
import okay.scala3.std.Async
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

/** THE JVM'S WORLD: okay-http's `Fetch` by the JDK's client */
object OnTheJvm:
  private lazy val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
  private val restricted = Set("host", "content-length", "connection", "upgrade", "expect")

  def fetching[R[_], Rest[_], A](p: A ! R)(using okay.scala3.Split[okay.http.Fetch, R, Rest], In[Async, Rest],
    ExecutionContext): A ! Rest =
    Fetching.by { (url, r) =>
      val b = HttpRequest.newBuilder(URI.create(url))
      for (name, values) <- r.headers if !restricted(name.toLowerCase); v <- values do b.header(name, v)
      val body = r.content.toArray
      b.method(r.method, if body.isEmpty then HttpRequest.BodyPublishers.noBody()
        else HttpRequest.BodyPublishers.ofByteArray(body))
      client.sendAsync(b.build(), HttpResponse.BodyHandlers.ofByteArray()).asScala.map { res =>
        val headers = res.headers().map().asScala.toMap.collect {
          case (k, vs) if !k.startsWith(":") => k.toLowerCase -> vs.asScala.mkString(", ")
        }
        Response(res.statusCode(), headers).withContent(Bytes(res.body()))
      }
    }(p)
