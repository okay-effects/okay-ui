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

   **A WAY OUT OF WORK (2026-10-27):** `interruptible(exits*)(block): Either[E, A]` — while `block` runs, every working
   screen shows `exits`, buttons of no input (`Button(label)(e)`); the block's value is `Right(a)` when it ends, and
   `Left(e)` when the person presses an exit: the block is left where it is — in the page its current wait cancelled
   (a `fetch` aborted, a timer cleared) — and the program goes on AFTER THE BLOCK with `Left(e)`, as if a `show` there
   had answered it. Two operations make it: `Exits(buttons): Option[E]`, answered `None` at once, and the same point
   resumed with `Some(e)` when an exit is pressed (a multi-shot `k`, kept); `Settled(): Unit`, the block's end, after
   which its exits are gone. Blocks nest: a working screen shows the exits of every block it is in, the innermost
   first, and pressing one leaves its own block. Exits are on working screens only: a `show` inside the block waits for
   its own buttons. `working` itself is unchanged: a working screen in no block has no button.

   WHY A BLOCK, not buttons on each working screen: progress is often drawn by a HANDLER (Coinpath's `Progress.toUi`,
   `Seen` into `Ui`) that cannot know where the program should go when the person leaves, nor jump there; the program
   says it once, around the work, and every working screen of that work — the program's own or a handler's — offers
   the way out. And the place it goes on from, the block's end, is a point the program names and handles
   (`case Left(another) => form`), not some point inside work half done.
4. **Steps**: the standard handler turns `A ! Ui` into `Step[A]` — `Done(a)`, `Waiting(screen, resume)` or
   `Working(screen, next)` — plain Scala, of no kernel. `resume` holds the program's continuation: resuming an older
   `Waiting` again is BACK (on a kernel with multi-shot continuations: all three above). `next` is the rest after a
   working screen, run when the toolkit has shown it.
   Inside an `interruptible` block a working screen is `Leavable(screen, next, leave)`: the screen's buttons are the
   exits (answering their number), `next()` goes on, `leave(i)` is the program after the block of exit `i`. A toolkit
   that cannot interrupt (a terminal, okay-faces' faces) goes on as at a `Working`.
5. **Toolkits**: anything that drives `Step`s. In this repository: `Play` (a test script, in core) and the
   okay-faces handler (module `faces`), which turns steps into a program of okay-faces' `Face` — so every okay-faces
   face (terminal, HTML, Swing, Laminar, MCP) runs a business application unchanged. That program of `Face` runs on
   okay-scala's machine too: okay-faces is written over okay-simple's interface, and okay-simple's adapter
   `okay-effects-cek` (`OnCek`) is that interface on okay-scala's CEK machine — `import okay.effects.cek.OnCek.self`.
   okay-simple's own machine is not needed, only its interface and the libraries okay-faces is made of.
6. **A program whose world answers LATER** — a browser's `fetch`, its timers, its storage, the person's click — is not
   driven by steps: in a page its answers come back later, and the screen is one of them. Asynchrony is an EFFECT,
   okay-scala's `Async`, driven by its own loop (`Async.toFuture`, `Async.start`), as okay-core does it — not a base
   monad at the end of the row. The browser's handlers of `Ui` on okay-scala's machine, `Laminar.run(root)` and
   `Dom.run(root)` (module `ui`, Scala.js), take `Ui` into a row with `Async` in it: `Show` draws the screen by
   okay-faces' face into `root` and is answered by the button pressed, `Async.async(cb => ...)`; `Working` draws its
   screen and goes on at once. The program's other handlers answer later the same way (`Async.cancellable`,
   `Async.sleep`, `Async.fromFuture` for a library's `Future`), and one run at the top drives it all:
   `Async.toFuture(Laminar.run(root)(Fetch.run(App.main)))`, or `Async.start(...)`, whose `Running` cancels it.
   A working screen in an `interruptible` block shows its exits; pressed, THE CURRENT WAIT IS CANCELLED — the
   operation's own cancel (`Async.cancellable`'s), so a `fetch` is aborted and a timer cleared — and the program goes
   on at the block's end with `Left(e)`; pressed while it is not waiting (it cannot be, in a page, between two of its
   steps), at its next wait. The handlers do it by intercepting `Async` around the program once, so each wait can be
   raced with the exit; the interception stays one deep however often a person leaves. With the deprecated
   `Lifted[Future]` a future cannot be cancelled: no exit is shown.
   In a test the same program is `steps` and `Play`: `Act.leave(label)` presses an exit on the next working screen
   that has it, and the script goes on from the block's end; any other act lets working screens go on.

   WHY ONE ASYNC EFFECT: any number of sources that answer later — fetches, timers, callbacks, a library's futures —
   are operations of one effect, with no terminal monad, so nothing has to be last; data monads (`Option`, `Either`,
   `List`) are handled where they stand, `Lift.handle`; and cancellation comes with it: a run cancelled cancels the
   operation it waits on — a `fetch` aborted, a timer cleared.

   The way before, `Ui` into okay-scala's `Lifted[Future]`, lowered once at the top
   (`Lift.lower(Laminar.run(root)(...))`), still compiles and runs, for one release: DEPRECATED. The handlers wait by
   a `Later[Rest]` — `Async` when the row has it, else a `Lifted[Future]` — so a program of either row is taken, the
   one way chosen by its row, nothing said.

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

## Points: where a program can be taken up again (2026-10-25)

`mark(point, state)` (`Ui.Mark`, `Step.Marked`): the program is at a point it can be taken up from, its state as
JSON text (the model stays free of a JSON library). A toolkit goes on (`Play`, `Dom.run`, `Laminar.run`); `OnFaces`
makes it okay-faces' own point (`Face.Mark`), which a host that loses its memory keeps instead of the way there
(okay-faces' specs/cloudflare.md). The reference application's menu is its point, its state the records
(specs/reference.md): a ledger of them starts it there again.

## A QR code (2026-10-25)

`Part.qr(text, alt)`: a QR code on a screen — an address for a phone's camera. The model holds the text alone; the
code is the toolkit's: `OnFaces` makes it okay-faces' `View.qr` (a picture, dark on light in either scheme), a
toolkit with no pictures says `alt` or the text.
