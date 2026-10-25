# okay-ui — business screens as data (spec)

Scala 3 only. Package `okay.business` (the platform okay has `okay.ui`; a split package would break JPMS if both were
ever on one module path).

## The idea

An application is written in the words a business user knows — a screen, a form, a field, a table, a button — and
nothing else. What a screen looks like, where it is drawn, how a click arrives: all of it is a HANDLER's. The same
program runs at a terminal, in a browser, in a window, for an agent, under a test script: the handler changes, the
program does not.

Under it, effects: the program is `A ! Ui` (plus the application's own effects), on any kernel of the okay family,
chosen by ONE import.

## Levels

1. **The kernel's words** (`Kernel`): `!`, `+`, `%`, `Pure`, `In`, `Split`, `Has`, `pure`, `flatMap`, `map`,
   `defer`, `perform`, `handle` (a `Handler`: `General` with `k`, or `Tail` answered in place), `run`. Abstract; an
   adapter is an object of this trait whose types ARE its kernel's own:
   - `okay.business.scala3` — okay-scala's CEK machine (the default);
   - `okay.business.simple` — okay-simple's machine; `okay.business.free` — okay-simple's `Free`;
   - more later (okay the platform, okay-direct) by the same trait.
2. **The model** (`okay.business.model`, exported by every adapter): values, no function of a toolkit.
3. **The effect `Ui`**: two operations. `Show(screen): A` — the screen shown, the result of the button pressed.
   `Working(screen): Unit` — a screen shown WHILE THE PROGRAM GOES ON, answered at once, no button: progress, "3 of
   at most 60 addresses read"; the next screen, of either kind, takes its place.
4. **Steps**: the standard handler turns `A ! Ui` into `Step[A]` — `Done(a)`, `Waiting(screen, resume)` or
   `Working(screen, next)` — plain Scala, of no kernel. `resume` holds the program's continuation: resuming an older
   `Waiting` again is BACK (on a kernel with multi-shot continuations: all three above). `next` is the rest after a
   working screen, run when the toolkit has shown it.
5. **Toolkits**: anything that drives `Step`s. In this repository: `Play` (a test script, in core) and the
   okay-faces handler (module `faces`), which turns steps into a program of okay-faces' `Face` — so every okay-faces
   face (terminal, HTML, Swing, Laminar, MCP) runs a business application unchanged. That program of `Face` runs on
   okay-scala's machine too: okay-faces is written over okay-simple's interface, and okay-simple's adapter
   `okay-effects-cek` (`OnCek`) is that interface on okay-scala's CEK machine — `import okay.effects.cek.OnCek.self`.
   okay-simple's own machine is not needed, only its interface and the libraries okay-faces is made of.
6. **A program whose world answers LATER** — a browser's `fetch`, its storage, the person's click — is not driven by
   steps: in a page every answer comes back as a `Future`, and the screen is one of those answers. The browser's
   handler of `Ui` on okay-scala's machine, `Laminar.run(root)` (module `ui`, Scala.js), takes `Ui` into okay-scala's
   `Lifted[Future]`: `Show` draws the screen by okay-faces' Laminar face into `root` and is answered by the button
   pressed, a `Future`; `Working` draws its screen and goes on at once. The program's other handlers lift their own
   futures the same way, and `Lift.lower` at the top makes one `Future` of it all:
   `Lift.lower(Laminar.run(root)(Fetch.run(App.main)))`. In a test the same program is `steps` and `Play`.

## The model

- `Day(year, month, day)` — a date, ISO text; no `java.time` (Scala.js).
- `Entry` — what a person entered: `Blank`, `Text`, `Number`, `Date`, `Flag`, `Pick(key)`. Toolkits produce entries;
  fields read them. A text entry is accepted by every kind (parsed), so a toolkit of text alone works.
- `Field[A]`: `key`, `label`, `kind` (`Text`, `LongText`, `Whole`, `Decimal`, `Money(currency)`, `Date`, `Flag`,
  `Choice(options)`, `Secret`), required or `optional`, `hint`, `initially(a)`, `check(message)(ok)`, `more` — one a
  person rarely changes, its initial value right for most (2026-10-24): on okay-faces it is the action's `more`, so
  the Laminar, DOM and HTML faces draw it under a closed "Options" at the end of the form, opened by a problem of it;
  the terminal and the others ask it as before. Nothing about its value changes. Constructors:
  `Field.text`, `longText`, `whole`, `decimal`, `money`, `date`, `flag`, `choice`, `secret`. The key is made from
  the label unless given.
- `Form[P]` — fields read together: `Form(name, amount, due)` is a `Form[(String, BigDecimal, Day)]`;
  `.as[Order]` by the case class's mirror; `.check(message)(ok)` across fields; `.filledWith(p)`.
- `Table[R]` — rows and `Column`s (`Column.text`, `number`, `money`, `date`, `flag`), each row a key.
- `Button[A]`: `Button(label)(a)`; `Button.submit(label, form)(p => a)` reads and checks the form;
  `Button.select(label, table)(r => a)` needs a selected row. A style: `Primary`, `Plain`, `Danger`.
- `Part` — what a screen holds: `Text`, `Notice(level)`, `Details`, a `Form`, a `Table`, a `Section`, a
  `Picture(width, height, shapes, text)` (2026-10-25) — a picture the program describes as data: `Shape.Box`,
  `Shape.Band` (a flow from a point to a point, so wide), `Shape.Label`, each shape marked with the application's
  word for its look, and the picture in words for a face with no pictures; on okay-faces its `View.Picture` (an
  `<svg>` in a page, the words at a terminal).
  `Link(label, url)` — a page elsewhere, a source to check (okay-faces' `View.Link`: an anchor in a page, `label
  <url>` at a terminal).
- `Screen[A](title)(parts*)(buttons*)`. `screen.press(answer)`: the button's result, or the problems — the same
  checking for every toolkit.
- `Answer(button, entries, selected)` — a button by its index, entries by (input, field key), selections by input;
  inputs are the screen's forms and tables numbered in order.

### Live screens (2026-10-25, for okay-guard's page)

A screen that changes while it is shown — a timeline of what an agent does, a call waiting for a person, a clock —
is still a screen, shown again; what is new is what may answer it and where its buttons stand:

- `Events[A]` — what happens meanwhile, of no toolkit: `Events(register)` (`register(k)` gives what stops it),
  `map`, `merge`, `Events.manual` (fired by hand: a test's, an application's bus). `screen.until(events)`: the
  screen answered by a button pressed OR by an event, whichever comes first. On okay-faces it is the step's
  `Meanwhile`; `Play` presses buttons only.
- `Part.Block(mark, title, parts)` — parts together under a mark (a card, a bubble, a bar): okay-faces'
  `View.Marked(mark, View.Group(title, ...))`, its look the skin's (`okay-mark-<mark>`). Forms and tables inside one
  are inputs as anywhere.
- `Part.Slot(name)` and `button.at(name)` — a button that stands where the screen says (Approve and Refuse in the
  card of the call they answer; the places of an application in its bar) instead of with the others. A button at a
  slot needs no input (a form's button stays with the forms); on okay-faces it is a live `Ref` (the action `ref`),
  drawn where its slot is, marked `button` and `button-<style>`. A slot not on the screen: the button with the
  others.
- `Kind.LongText` is a text area (okay-faces' `Action.long`).

In a page the screens of one program are ONE face (`Dom.run(root, skin)`, okay-faces' DOM face): a screen that
follows another with the same buttons changes the page in place — what was typed stays, the focus, the scroll;
`Dom.run(root, skin, follow = true)` keeps the end in view as a screen grows, when a person was at the end.

## The words

`show(screen)`, `inform(title, text)`, `confirm(question)`, `edit(title, form)`, `pick(title, table)`,
`menu(title, options*)` — each `A ! R` for any `R` that `Has[Ui]`; `working(title)(parts*)` — a screen shown while
the program goes on. `steps(p)` the standard handler.

## Contract of a toolkit

A toolkit shows `Waiting.screen`, lets the person fill its forms, select in its tables and press a button, builds an
`Answer`, and calls `Waiting.answer`: `Left(problems)` — show them and stay; `Right(step)` — go on. It never decides
what a field accepts. A `Working` it shows and goes on from at once (`next()`); `Play` counts its title as seen.

## How a page looks

A page's look is okay-ui's, built in: a `Skin` (module `faces`, a value of no toolkit) is okay-faces' `Look` — the
tokens and per-part rules every face reads, and its words — and a stylesheet over okay-faces' classes for what tokens
cannot say: cards, radii, shadows, a grid of fields, a dark scheme. Nothing in it is content.

- `Skin.default` = `Skin.calm()`: forms and tables as cards, the fields side by side on a wide screen, the actions
  that go back (`back`, `cancel`, `ok`) outlined, light or dark by the system's choice.
  `Skin.calm(accent, deep, scheme)` — the application's colours (the accent of buttons and links, the deep one of the
  page's bar) and `Scheme.Auto` (the system's), `Light` or `Dark`.
- `Skin.plain` — okay-faces' own look alone, no stylesheet of okay-ui's.
- In a page (module `ui`, Scala.js) the skin is the default with nothing said: `Laminar.run(root)` and
  `Page.mount(program)` put `Skin.default` into the document; another is one argument,
  `Page.mount(program, Skin.calm(accent = "#0b7a5c"))`. `Page.install(skin)` alone dresses a page drawn otherwise;
  installed again it replaces the one before.
- The stylesheet also dresses a page's frame if the page has one: a `<header>` and a `<footer>` that are children of
  `<body>`, each with a `.bar` inside (a `.brand` and a `.tag` in the header). The application's page writes them;
  the skin only says how they look.

## Modules

| module | artifact | depends on |
|---|---|---|
| `core` | `okay-ui-core` | nothing |
| `scala3` | `okay-ui-scala3` | core, okay-scala |
| `simple` | `okay-ui-simple` | core, okay-simple (machine, free) |
| `faces` | `okay-ui-faces` | core, okay-faces, okay-simple's `okay-effects-cek` (okay-scala's machine); `Skin` |
| `ui` | `okay-ui` | `scala3`, `faces`; in a page (Scala.js, okay-faces' Laminar face): `Laminar.run`, `Page` |
| `examples` | — | all |
| `reference` | — | `scala3`, `faces`: the reference application, [reference.md](reference.md) |

The default application depends on `okay-ui-scala3` and `okay-ui-faces`: the business program and its screens both
on okay-scala's machine.

## Tests

The same example application on every kernel, under `Play` and under okay-faces' test face `Plays`; the one program
word for word with only its import changed.
