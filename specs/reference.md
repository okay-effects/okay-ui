# The reference application: Expenses (spec)

A small personal expense tracker, written to show one rule end to end. Module `reference`, package
`okay.business.reference`, on okay-scala (`import okay.business.scala3.*`), JVM and Scala.js.

## The rule

The architecture is effects. The DATA MODEL is effects: what the program asks, and what it is answered. Every
IMPLEMENTATION is a HANDLER. Parts exchange data only as effect operations. Each environment runs the whole application
in ONE LINE that names its handlers. Tests swap handlers: no mocks, no flags.

## The effects

| effect | operation | answered with | defined by |
|---|---|---|---|
| `Ledger` | `Entries()` | `List[Expense]`, in the order added | the application |
| | `Add(e: Expense)` | `Unit` | |
| | `Remove(id: Long)` | `Unit`; an unknown id is no change | |
| `Today` | `Now()` | `Day` | the application |
| `Ui` | `Show(screen)` | the result of the button pressed | okay-ui |

`Expense(id: Long, day: Day, amount: BigDecimal, category: String, note: Option[String])`. The id is the program's: one
more than the largest kept (1 for the first), so each handler stores and nothing else. The program never reads a clock,
a file or the page: it asks `Today` and `Ledger`.

## The program

`Expenses.main[R[_]: Has[Ui]: Has[Ledger]: Has[Today]]: Unit ! R`, a menu for as long as the application is open:

- **Add an expense**: a form. Amount (money, more than zero), Category (a choice of Food, Transport, Home, Health, Fun,
  Other), Day (a date, starting at today), Note (optional). A rejected value stays on the form, its problem at its
  field.
- **This month**: the month that `Today` says. Its expenses in a table, by day, the totals by category in a second
  table, and the month's total.
- **All expenses**: every expense in a table, newest first. A row selected and opened shows its details. Remove asks
  for a confirmation, then removes it.
- No **Quit**: in a page it would only leave the page empty; a terminal ends the way a terminal does (Ctrl-C, the end
  of input).

The program uses okay-ui's model words only: `menu`, `edit`, `show`, `inform`, `confirm`, `Screen`, `Form`, `Field`,
`Table`, `Column`, `Button`, `Part`.

## The handlers

Each handler is a small class of its own (`InMemory`, `InFile`, `InLocalStorage`, `Clock`), and it is the only place
that knows its world. `Today` has one class, `Clock`: it is given a fixed day, or the platform's clock.

| handler | effect | world | environment |
|---|---|---|---|
| `Ledger.inMemory(initial)` | `Ledger` | a variable | tests |
| `Ledger.file(path)` | `Ledger` | a JSON file, read and rewritten whole | the JVM: the terminal, tests |
| `Ledger.localStorage(key)` | `Ledger` | `window.localStorage[key]`, JSON | Scala.js: the browser, the page test |
| `Today.fixed(day)` | `Today` | none: always `day` | tests |
| `Today.system` | `Today` | the system clock: `java.time`, `js.Date` | the terminal, the browser |
| `steps` | `Ui` | none: a `Step` for a toolkit to drive | okay-ui, everywhere |

A handler takes `E` out of any program's row: `apply[R, Rest, A](p: A ! R)(using Split[E, R, Rest]): A ! Rest`. The
JSON handlers share one format (`Expense.write`, `Expense.read`). Empty or missing text is no expenses. Text that is not
a list of expenses is also no expenses, so a corrupt value does not stop the application. A record in the list that
cannot be read is skipped. Platform handlers live in `scala-jvm` and `scala-js` source directories. `Ledger` and
`Today` get them through a trait that each platform defines.

## The one-line runs

```scala
// the browser (Scala.js)
LaminarFace.mount(OnFaces.program(steps(Ledger.localStorage("expenses")(Today.system(Expenses.main[App])))), settings)
// the terminal (JVM)
okay.faces.run(Terminal.console(OnFaces.program(steps(Ledger.file(path)(Today.system(Expenses.main[App]))))))
// a test
Play(steps(Ledger.inMemory(Nil)(Today.fixed(day)(Expenses.main[App]))), acts*)
// Cloudflare Workers (Scala.js, reference/cloudflare): each session a Durable Object, its pages and its API
Cloudflare.serve(Door.html(expenses, pages), Door.rest(expenses))
//   where expenses = OnFaces.program(steps(Ledger.inMemory(Nil)(Today.system(Expenses.main[App]))))
```

ON CLOUDFLARE (`OnCloudflare`, okay-faces' `web/cloudflare`, its spec `specs/cloudflare.md` there): a deploying, not a
change — the program is the same; okay-faces' HTML and REST faces serve it, each session a Durable Object. The ledger
is the session's, in memory (`Door` makes the program again for each session), and it lasts as the session does: the
session's journal of requests, played when the object wakes, adds the same expenses again. `reference/cloudflare/
deploy.sh` (`wrangler dev`, or `deploy`), `e2e.js` (two expenses, the Worker started again, both there; another
session has none). Not yet: a ledger shared by a person's sessions (a `Ledger` over the object's storage, its answers
later), the date journaled.

`App = Ui + Ledger + Today`. Read from the inside out: the program, `Today` handled, `Ledger` handled, `Ui` turned
into steps, the steps driven by a toolkit.

## Tests

One script for all of them, shared by the JVM and Scala.js:

- expenses added, then the month's totals by category with a fixed `Today`; an expense of another month is not in it;
- a rejected amount (zero, three places) stays on the form, its problem at its field, and nothing is kept;
- the day starts at today when it is left as it is;
- removal asks for confirmation; No keeps the expense, Yes removes it;
- the same script against `Ledger.inMemory` and `Ledger.file` in a temporary directory (JVM), and against
  `Ledger.localStorage` (Scala.js), gives the same records. That is the handler swap.
- the format: what is written reads back; empty, corrupt and partly wrong text read as stated above.
- the browser line under jsdom, as `ShopPageSuite` does: an expense added on the page is in `localStorage`, and a page
  mounted again (a reload) shows it.

## Added to the library

Nothing in the model or the trait. Everything the application needed was already there: a starting value is
`Field.initially` (the day starts at today, and every face shows it), a choice is `Field.choice`, and totals are a
second `Table` in a `Part.section`. An addition would have gone into the model or the `Business` trait, for every
kernel.

The build gained `whenJs` (project/WhenJs.scala). A module's Scala.js settings are set only when Scala.js is asked.
Before it, `jsSettings` on a module without its JS project stopped the build from loading, so a plain `sbt test`
(JVM only) failed.

## Fixed: decimals in a real browser (okay-faces 0.1.0, republished)

The first okay-faces 0.1.0's Laminar face drew a number field as `<input type="number">` without `step`, so a browser refused
a decimal such as `12.5` before the form was sent (its own validation, step 1); jsdom does not validate, so the page
tests passed. Found by this application's headless run in Chromium; fixed in okay-faces (`step="any"` on a decimal
input, in the Laminar and the HTML faces, with a test) and 0.1.0 republished with it — nobody had taken it yet.
