package okay.business.reference

import okay.business.faces.OnFaces
import okay.business.reference.Expenses.App
import okay.business.scala3.steps
import okay.effects.cek.OnCek.self
import okay.effects.json.Json
import okay.faces.Step.Point
import okay.faces.cloudflare.{Cloudflare, Door}
import okay.faces.html.HtmlFace

/** EXPENSES ON CLOUDFLARE WORKERS: `reference/cloudflare/deploy.sh` (wrangler) — the thin page at `/` (told by its
 * session's socket when it moved), its pages at `/s`, its API at `/sessions`, its tools for an agent at `/mcp`, each
 * session a Durable Object (okay-faces' `web/cloudflare`). What a session keeps is its STATE, not the way there: the
 * menu's point (`Expenses.point`) carries the records, and a session woken starts at the menu with a ledger of them
 * (`from`); the date from the Worker's clock. The whole application is the line below, the same program as
 * `InTheBrowser`'s and `AtTheTerminal`'s */
object OnCloudflare:
  def main(args: Array[String]): Unit = Cloudflare.serve(Door.shell("Expenses", Cloudflare.asset("/index.js"), look),
    Door.html(expenses(Nil), HtmlFace.Settings(look = look), atRoot = false, from = from),
    Door.rest(expenses(Nil), from = from), Door.mcp(expenses(Nil), from = from))

  /** made for each session, its ledger its own: empty, or the records a point kept */
  def expenses(records: List[Expense]) =
    OnFaces.program(steps(Ledger.inMemory(records)(Today.system(Expenses.main[App]))))
  /** the program at the menu's point: a ledger of the records it carries */
  def from(p: Point) = Option.when(p.name == Expenses.point)(expenses(Expense.read(Json.print(p.state))))
  val look = Some(InTheBrowser.skin.look)
