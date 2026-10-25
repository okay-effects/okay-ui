# AGENTS.md — okay-ui

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
- `ui` with Scala.js has `Laminar.run(root)`: `Ui` into okay-scala's `Lifted[Future]`,
  for programs whose world answers later (Coinpath's fetches); its test under jsdom.
- A page's look is okay-ui's, built in (spec, "How a page looks"): `Skin` in `faces` (okay-faces' `Look` plus a
  stylesheet over its classes), `Skin.default` = `calm()`, `Skin.plain`; in a page `Page.mount` / `Laminar.run(root)`
  install it with nothing said. A page's `index.html` writes only its frame (header, `#app`, footer), never a style.
- The reference application (`reference/`, specs/reference.md) shows the rule: data model as effects, every
  implementation a handler, one line per environment. Keep its mains one line each.
- A module's Scala.js settings go in `.whenJs(...)` (project/WhenJs.scala), never `.jsSettings`: without
  `-Dplatforms=js` there is no JS project, and `jsSettings` stops the build from loading.
- `sbt test`. Lines at most 120 wide. Commits: plain messages,
  no attribution trailers.
