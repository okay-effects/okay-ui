# okay-ui — business applications, their screens as data

A business application written in the words its users know: a screen, a form, a field, a table, a button. Nothing
else. Where a screen is drawn, what it looks like, how a click comes back is a HANDLER's: the same program runs at a
terminal, in a browser, in a window, as an agent's tools, under a test script. Change the handler, not the program.

Scala 3 only. Package `okay.business`. Under it, algebraic effects, on any kernel of the okay family, chosen by one
import.

```scala
import okay.business.scala3.*          // the kernel: okay-scala's machine. `.simple.*`, `.free.*`: okay-simple's

final case class Customer(name: String, email: String, vip: Boolean)

val customerForm: Form[Customer] =
  Form(Field.text("Name"), Field.text("Email").check("an e-mail address")(_.contains("@")), Field.flag("VIP"))
    .as[Customer]

def addCustomer[R[_]: Has[Ui]]: Option[Customer] ! R =
  edit("New customer", customerForm).flatMap {
    case Some(c) => inform("Saved", s"${c.name} added").as(Some(c))
    case None => pure(None)
  }
```

Run it anywhere: its steps (`steps(program)`), driven by a toolkit.

```scala
okay.faces.run(Terminal.console(OnFaces.program(steps(addCustomer[Ui]))))   // okay-faces' terminal
Play(steps(addCustomer[Ui]), Act.press("Save", "Name" -> "Ann", "Email" -> "ann@example.com"), Act.press("OK"))
                                                                             // a test, by labels
```

## Three levels

1. **The kernel's words** — `!`, `+`, `%`, `Pure`, `Has`, `In`, `Split`, `pure`, `flatMap`, `map`, `defer`,
   `perform`, `handle` (a `Handler.General` with `k`, or a `Handler.Tail` answering in place), `run`. One trait,
   `Kernel`; each adapter an object of it whose types ARE its kernel's own:

   | import | kernel |
   |---|---|
   | `okay.business.scala3.*` | okay-scala's CEK machine — the default |
   | `okay.business.simple.*` | okay-simple's machine |
   | `okay.business.free.*` | okay-simple's `Free` |

   The example shop is written once; the build makes its copies for the other kernels by changing the import line
   (and the package) alone, and the same tests pass on all three.

2. **The model** — `Field` (`text`, `longText`, `secret`, `whole`, `decimal`, `money`, `date`, `flag`, `choice`;
   `optional`, `check`, `initially`), `Form` (`Form(f1, f2, ...)` a tuple, `.as[CaseClass]`, `.check` across fields,
   `.filledWith`), `Table` with `Column`s, `Button` (`apply`, `submit`, `select`), `Part` (text, notices, details,
   sections), `Screen`. Values, no toolkit. What a field accepts is decided here, once, for every toolkit.

3. **The effect `Ui`** — one operation, a screen shown, answered by the button pressed. Words: `show`, `inform`,
   `confirm`, `edit`, `pick`, `menu`. Its standard handler `steps` makes `Step`s — `Done`, or `Waiting(screen,
   resume)` — of no kernel: what every toolkit drives. Resuming an older `Waiting` again is BACK.

## Toolkits

- **`Play`** (core): a person by script — buttons by label, fields by label, rows by their text.
- **okay-faces** (module `faces`, `OnFaces.program`): steps as a program of okay-faces' `Face`, so each of its faces
  runs a business application — terminal, HTML, Swing, Laminar, MCP. A field is a `Shape` that reads the business
  field's way, so its problem is shown at the field.
- Another toolkit is a loop over `Step`: show the screen, read an `Answer`, call `answer`. See
  [specs/business.md](specs/business.md).

## The reference application

[Expenses](reference/src/main/scala/okay/business/reference), a small personal expense tracker, shows the
architecture end to end. The data model is effects: what the program asks, and what it is answered. Every
implementation is a handler, and each environment runs the whole application in one line that names its handlers.

| effect | operations | defined by |
|---|---|---|
| `Ledger` | `Entries(): List[Expense]`, `Add(e): Unit`, `Remove(id): Unit` | the application |
| `Today` | `Now(): Day` | the application |
| `Ui` | `Show(screen)`: the button pressed | okay-ui |

| handler | effect | where it runs |
|---|---|---|
| `Ledger.inMemory(initial)` | `Ledger` | tests |
| `Ledger.file(path)` | `Ledger` | the JVM: a JSON file |
| `Ledger.localStorage(key)` | `Ledger` | the browser: `window.localStorage` |
| `Today.fixed(day)` | `Today` | tests |
| `Today.system` | `Today` | the JVM (`java.time`), the browser (`js.Date`) |
| `steps` | `Ui` | everywhere: steps for a toolkit to drive |

The program, `Expenses.main[R[_]: Has[Ui]: Has[Ledger]: Has[Today]]: Unit ! R`, never reads a clock, a file or the
page. It asks. The three environments differ only in the handlers they name:

```scala
// the browser
LaminarFace.mount(OnFaces.program(steps(Ledger.localStorage("expenses")(Today.system(Expenses.main[App])))), page)
// the terminal
okay.faces.run(Terminal.console(OnFaces.program(steps(Ledger.file(path)(Today.system(Expenses.main[App]))))))
// a test
Play(steps(Ledger.inMemory(Nil)(Today.fixed(day)(Expenses.main[App]))), acts*)
```

Tests swap handlers, with no mocks and no flags. One script runs against the ledger in memory, in a file (JVM) and in
local storage (Scala.js), and gives the same records. The page is tested under jsdom.

```text
sbt "referenceJVM/run expenses.json"        # at a terminal, the records in expenses.json
sbt -Dplatforms=jvm,js referenceJS/web      # in a browser: open reference/.js/target/web/index.html
```

The spec, with what was found on the way: [specs/reference.md](specs/reference.md).

## Modules

| module | artifact | depends on |
|---|---|---|
| `core` | `okay-ui-core` | nothing |
| `scala3` | `okay-ui-scala3` | okay-scala |
| `simple` | `okay-ui-simple` | okay-simple (machine, Free) |
| `faces` | `okay-ui-faces` | okay-faces |
| `ui` | `okay-ui` | the default: `scala3` and `faces` |
| `examples` | — | the shop, on every kernel and on okay-faces: at a terminal (JVM), in a page (Scala.js) |
| `reference` | — | the reference application, Expenses: terminal (JVM), page (Scala.js) |

JVM and Scala.js (`sbt -Dplatforms=jvm,js`).

## Build

okay-scala 0.1.0 is released. okay-simple's adapter onto okay-scala's machine, `okay-effects-cek`, is not yet: publish
it locally from okay-simple (AGENTS.md says how).

```text
sbt test                                   # the model, the shop on three kernels, on okay-faces and its terminal
sbt examplesJVM/run                        # the shop at a terminal
sbt -Dplatforms=jvm,js test                # the same on Scala.js too, the shop's page clicked through under jsdom
sbt -Dplatforms=jvm,js examplesJS/web      # the shop in a browser: open examples/.js/target/web/index.html
```

License: Apache 2.0.
