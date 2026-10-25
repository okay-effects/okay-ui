package okay.business.reference

import okay.business.faces.OnFaces
import okay.business.reference.Expenses.App
import okay.business.scala3.steps
import okay.effects.cek.OnCek.self
import okay.faces.cloudflare.{Cloudflare, Door}
import okay.faces.html.HtmlFace

/** EXPENSES ON CLOUDFLARE WORKERS: `reference/cloudflare/deploy.sh` (wrangler) — the thin page at `/` (told by its
 * session's socket when it moved), its pages at `/s`, its API at `/sessions`, its tools for an agent at `/mcp`, each
 * session a Durable Object (okay-faces' `web/cloudflare`): the records the session's, in memory, kept by its journal;
 * the date from the Worker's clock. The whole application is the line below, the same program as `InTheBrowser`'s and
 * `AtTheTerminal`'s */
object OnCloudflare:
  def main(args: Array[String]): Unit = Cloudflare.serve(Door.shell("Expenses", Cloudflare.asset("/index.js"), look),
    Door.html(expenses, HtmlFace.Settings(look = look), atRoot = false), Door.rest(expenses), Door.mcp(expenses))

  /** made for each session: its ledger its own */
  def expenses = OnFaces.program(steps(Ledger.inMemory(Nil)(Today.system(Expenses.main[App]))))
  val look = Some(InTheBrowser.skin.look)
