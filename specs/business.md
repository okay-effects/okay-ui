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
3. **The effect `Ui`**: one operation, `Show(screen): A` — the screen shown, the result of the button pressed.
4. **Steps**: the standard handler turns `A ! Ui` into `Step[A]` — `Done(a)` or `Waiting(screen, resume)` — plain
   Scala, of no kernel. `resume` holds the program's continuation: resuming an older `Waiting` again is BACK
   (on a kernel with multi-shot continuations: all three above).
5. **Toolkits**: anything that drives `Step`s. In this repository: `Play` (a test script, in core) and the
   okay-faces handler (module `faces`), which turns steps into a program of okay-faces' `Face` — so every okay-faces
   face (terminal, HTML, Swing, Laminar, MCP) runs a business application unchanged.

## The model

- `Day(year, month, day)` — a date, ISO text; no `java.time` (Scala.js).
- `Entry` — what a person entered: `Blank`, `Text`, `Number`, `Date`, `Flag`, `Pick(key)`. Toolkits produce entries;
  fields read them. A text entry is accepted by every kind (parsed), so a toolkit of text alone works.
- `Field[A]`: `key`, `label`, `kind` (`Text`, `LongText`, `Whole`, `Decimal`, `Money(currency)`, `Date`, `Flag`,
  `Choice(options)`, `Secret`), required or `optional`, `hint`, `initially(a)`, `check(message)(ok)`. Constructors:
  `Field.text`, `longText`, `whole`, `decimal`, `money`, `date`, `flag`, `choice`, `secret`. The key is made from
  the label unless given.
- `Form[P]` — fields read together: `Form(name, amount, due)` is a `Form[(String, BigDecimal, Day)]`;
  `.as[Order]` by the case class's mirror; `.check(message)(ok)` across fields; `.filledWith(p)`.
- `Table[R]` — rows and `Column`s (`Column.text`, `number`, `money`, `date`, `flag`), each row a key.
- `Button[A]`: `Button(label)(a)`; `Button.submit(label, form)(p => a)` reads and checks the form;
  `Button.select(label, table)(r => a)` needs a selected row. A style: `Primary`, `Plain`, `Danger`.
- `Part` — what a screen holds: `Text`, `Notice(level)`, `Details`, a `Form`, a `Table`, a `Section`.
- `Screen[A](title)(parts*)(buttons*)`. `screen.press(answer)`: the button's result, or the problems — the same
  checking for every toolkit.
- `Answer(button, entries, selected)` — a button by its index, entries by (input, field key), selections by input;
  inputs are the screen's forms and tables numbered in order.

## The words

`show(screen)`, `inform(title, text)`, `confirm(question)`, `edit(title, form)`, `pick(title, table)`,
`menu(title, options*)` — each `A ! R` for any `R` that `Has[Ui]`. `steps(p)` the standard handler.

## Contract of a toolkit

A toolkit shows `Waiting.screen`, lets the person fill its forms, select in its tables and press a button, builds an
`Answer`, and calls `Waiting.answer`: `Left(problems)` — show them and stay; `Right(step)` — go on. It never decides
what a field accepts.

## Modules

| module | artifact | depends on |
|---|---|---|
| `core` | `okay-ui-core` | nothing |
| `scala3` | `okay-ui-scala3` | core, okay-scala |
| `simple` | `okay-ui-simple` | core, okay-simple (machine, free) |
| `faces` | `okay-ui-faces` | core, okay-faces |
| `examples` | — | all |

The default application depends on `okay-ui-scala3` and `okay-ui-faces`.

## Tests

The same example application on every kernel, under `Play` and under okay-faces' test face `Plays`; the one program
word for word with only its import changed.
