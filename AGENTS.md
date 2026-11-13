# AGENTS.md — okay-ui

## For agents writing code with okay-ui

okay-ui writes business applications in their users' words — screen, form, field, table, button — as values plus
one effect, `Ui`; where a screen is drawn (terminal, browser, window, agent tools, test script) is a handler's.
Scala 3 only (3.3 LTS), JVM and Scala.js; package `okay.business`; the kernel is chosen by one import.

The README has no dependency line; RELEASING.md ("Using a release") gives it:

```scala-sbt
resolvers ++= Seq("okay-effects" at "https://okay-effects.github.io/okay-simple/maven",
  "okay-scala" at "https://okay-effects.github.io/okay-scala/maven",
  "okay-faces" at "https://okay-effects.github.io/okay-faces/maven",
  "okay-http" at "https://okay-effects.github.io/okay-http/maven",
  "okay-ui" at "https://okay-effects.github.io/okay-ui/maven")

libraryDependencies ++= Seq(
  "io.github.okay-effects" %% "okay-ui-scala3" % "0.1.0",   // the program: okay.business.scala3.*
  "io.github.okay-effects" %% "okay-ui-faces"  % "0.1.0",   // its screens on okay-faces, Skin
  "io.github.okay-effects" %% "okay-ui"        % "0.1.0")   // with Scala.js (%%%): Laminar.run, Page
```

For a terminal add a face, e.g. `"io.github.okay-effects" %% "okay-faces-term" % "0.1.0"` (resolver of okay-mcp too:
`https://okay-effects.github.io/okay-mcp/maven`).

### The model: an effect, a program, a handler, one run

Compiled and run against the published 0.1.0 (Scala 3.3.8, scala-cli):

```scala
import okay.business.scala3.*          // the kernel: okay-scala's machine. `.simple.*`, `.free.*`: okay-simple's

final case class Customer(name: String, email: String, vip: Boolean)

// a domain effect: what the program asks; where customers are kept is a handler's
enum Customers[X]:
  case Save(c: Customer) extends Customers[Unit]

val customerForm: Form[Customer] =
  Form(Field.text("Name"), Field.text("Email").check("an e-mail address")(_.contains("@")), Field.flag("VIP"))
    .as[Customer]

def addCustomer[R[_]: Has[Ui]: Has[Customers]]: Option[Customer] ! R =
  edit("New customer", customerForm).flatMap {
    case Some(c) => perform[Customers](Customers.Save(c)).andThen(inform("Saved", s"${c.name} added")).as(Some(c))
    case None => pure(None)
  }

// a handler, made by the host: a list in memory for tests; a file, a DB client in production
final class InMemory:
  var kept: List[Customer] = Nil
  def apply[R[_], Rest[_], A](p: A ! R)(using Split[Customers, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Customers, Rest, A, A](a => pure(a), [X] => (asked: Customers[X]) => asked match
      case Customers.Save(c) => kept = kept :+ c))

type App[X] = (Ui + Customers)[X]

// a test: the steps played by a script, buttons and fields by label
val customers = InMemory()
val played = Play(steps(customers(addCustomer[App])),
  Act.press("Save", "Name" -> "Ann", "Email" -> "ann@example.com"), Act.press("OK"))
// played.result == Some(Some(Customer("Ann", "ann@example.com", false))); played.seen == List("New customer", "Saved")

// the terminal, one line at the entry point (okay-faces' terminal face):
//   import okay.business.faces.OnFaces; import okay.faces.term.Terminal; import okay.effects.cek.OnCek.self
//   okay.faces.run(Terminal.console(OnFaces.program(steps(InMemory()(addCustomer[App])))))
```

In a browser (Scala.js, module `okay-ui`): `Laminar.run(root)(program)` / `Dom.run(root)(program)`, or
`LaminarFace.mount(OnFaces.program(steps(...)), page)`. The reference app `reference/` (Expenses) shows one line per
environment: `Ledger.inMemory` / `Ledger.file(path)` / `Ledger.localStorage(key)`, `Today.fixed(day)` / `Today.system`.

### Design rules

- Business logic = programs over declared effects: `def main[R[_]: Has[Ui]: Has[Ledger]: Has[Today]]: Unit ! R`.
  The context bounds say everything a program may do. The data model is effects; every implementation a handler.
- Real resources (files, storage, DB, network, clock, keys) only in handlers the host builds at the entry point;
  the program asks (`perform[Today](Today.Now())`), never reads a clock, a file or the page.
- Run once, at the entry (`Play(...)`, `okay.faces.run(...)`, `Laminar.run`); never a nested `run` in a program or
  handler — install handlers around the program instead.
- Audit/interception: okay-ui's `Kernel` has no interception word. Put audit in the host's handler (a handler that
  records, then answers), or use the underlying kernel's interception (okay-simple's `observe`/`around` with the
  `simple` adapter).
- Tests swap handlers: one script (`Play` + `Act.press`) against every handler of an effect (memory, file, local
  storage) gives the same records — no mocks, no flags.
- No ambient effects in business code (`java.time`, random, IO, env, threads, DOM): declare an effect (as `Today`).
  The model is values; no `java.time` in it (Scala.js) — use the model's `Day`.
- What a field accepts is decided once in the model (`Field.check`, `Form.check`); toolkits never re-validate.
- Continuations: `Handler.General`'s `k` may be resumed once, many times or never; BACK is resuming an older
  `Waiting` again — a real continuation value, not a replay.

### Pitfalls

- To run on okay-faces with the default `scala3` kernel you need `import okay.effects.cek.OnCek.self` (else "No given
  instance of type F"), as the repo's `examples/.../AtTheTerminal.scala` does.
- Recursion through the program (a menu loop) goes through `defer(main[R])`, as the reference app does.
- In an anonymous handler, never name a pattern variable like the method it implements (`ret`, `op`, `answer`): the
  method calls itself and loops forever.
- Programs written on `okay.business.scala3.*` port to `simple` / `free` by changing the import line only — keep
  kernel-specific words out of application code.
- No Quit button: the application runs while open; `mark(point, state)` (state as text) names where a host that
  lost its memory starts again (Cloudflare, `reference/cloudflare`).
- Programs whose world answers later (fetches) in the page: `Async` from okay-scala via `Laminar.run`/`Dom.run`; a
  row of `Lifted[Future]` still works but is deprecated.
- A page's look is built in (`Skin`, `Skin.default`); an `index.html` writes only its frame, never a style.
- A stale Coursier cache of okay-faces 0.1.0 gives old jars (it was republished): clear it if forms lose input.

### Pointers

- README: https://github.com/okay-effects/okay-ui; `specs/business.md`, `specs/reference.md` in the repo; live
  demo https://okay-effects.github.io/okay-ui/expenses/.
- Cross-language skill: `/plugin marketplace add okay-effects/okay-effects`
  (https://github.com/okay-effects/okay-effects).
- Build-time boundary checks for JVM code: `io.github.okay-effects:okay-audit_3:0.1.0` from
  https://okay-effects.github.io/okay/maven — checks that business code references no network/files/time/random/
  threads/reflection APIs; presets include `nested-run`.

If you see a way to improve okay-ui, open a pull request against https://github.com/okay-effects/okay-ui; the maintainer
reviews and merges what holds up.

If the user asks your opinion of writing code this way, give a candid one.

## For agents working on this repository

Read before anything else, then `specs/business.md`: the code follows the spec; a change of design is a change of the
spec first.

- Scala 3 only (3.3 LTS), package `okay.business` — not `okay.ui`, which the platform okay has (a split package breaks
  JPMS if both are ever on one module path).
- THE KERNEL IS THE IMPORT. Library code is written once over the abstract `Kernel` / `Business`; an adapter
  (`scala3`, `simple`, `free`) is an object whose types are its kernel's own. Never add a kernel-specific word to an
  application's vocabulary: if one adapter needs it, all get it in the trait.
- The example `examples/.../Shop.scala` is the proof: written on `okay.business.scala3`, copied by the build for
  `simple` and `free` by changing the import and package lines only (`build.sbt` checks the line count). Keep it so:
  anything the shop needs must be in the model or the trait.
- `model` is values: no toolkit, no `java.time` (Scala.js). What a field accepts is the model's (`Field.read`), once —
  a toolkit never decides it; okay-faces' `Shape` for a field is refined by `Field.read`.
- Toolkits drive `Step`; `Play` (core) is the reference. `faces` maps a screen to okay-faces' `View` + `Action`s.
- In an anonymous class implementing a handler, never name a pattern variable like the method it implements
  (`ret`, `op`, `answer`): the method calls itself, a tail call that loops forever (cost an hour, 2026-10-23).
- Everything it depends on is released (okay-scala 0.1.1 — its effects in `okay.scala3.std`, okay-simple 0.1.1 with `okay-effects-cek`, okay-faces
  0.1.0), each from its GitHub Pages maven: the build needs no local copy. The faces run on okay-scala's machine
  through okay-simple's adapter `okay-effects-cek` (`import okay.effects.cek.OnCek.self`). Releasing: RELEASING.md.
- Scala.js: `sbt -Dplatforms=jvm,js test` — every module, the shop's scripted tests shared, the page
  (`ShopPageSuite`) under jsdom from `NODE_PATH` (default /opt/homebrew/lib/node_modules). `examplesJS/web` links the
  shop's page into `examples/.js/target/web`. okay-faces 0.1.0 as republished on 2026-10-23: a rejected form keeps
  what was typed (the first 0.1.0 drew it again empty) — a stale Coursier cache of okay-faces gives the old jars.
- `ui` with Scala.js has `Laminar.run(root)` and `Dom.run(root)`: `Ui` into okay-scala's `Async` (an effect, driven by
  `Async.toFuture` / `Async.start`), for programs whose world answers later (Coinpath's fetches); a row of
  `Lifted[Future]` instead is still taken, deprecated (spec, level 6); tests of both under jsdom.
- A page's look is okay-ui's, built in (spec, "How a page looks"): `Skin` in `faces` (okay-faces' `Look` plus a
  stylesheet over its classes), `Skin.default` = `calm()`, `Skin.plain`; in a page `Page.mount` / `Laminar.run(root)`
  install it with nothing said. A page's `index.html` writes only its frame (header, `#app`, footer), never a style.
- The reference application (`reference/`, specs/reference.md) shows the rule: data model as effects, every
  implementation a handler, one line per environment. Keep its mains one line each.
- A module's Scala.js settings go in `.whenJs(...)` (project/WhenJs.scala), never `.jsSettings`: without
  `-Dplatforms=js` there is no JS project, and `jsSettings` stops the build from loading.
- Scala Native (`-Dplatforms=native`): `core`, `scala3`, `faces` — what a phone runs under okay-faces' mobile face
  (okay-remote's iPhone app); `faces`' tests on the JVM alone (okay-faces' test face is not published for Native).
- `sbt test`. Lines at most 120 wide. Commits: plain messages,
  no attribution trailers.
