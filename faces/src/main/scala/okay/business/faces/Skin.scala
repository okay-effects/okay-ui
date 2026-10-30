package okay.business.faces

import okay.faces.{Color, Look, Theme}

/** light or dark: the system's choice, or one of the two always */
enum Scheme:
  case Auto, Light, Dark

/**
 * HOW A PAGE LOOKS, okay-ui's own (specs/business.md, "How a page looks"): okay-faces' `Look` — the tokens and
 * per-part rules every face reads, and its words — and `css`, a stylesheet over okay-faces' classes for what tokens
 * cannot say: cards, radii, shadows, a grid of fields, a dark scheme. Nothing in it is content. A page puts `css`
 * into its document (`Page.install` in module `ui`); its rules are under `body` so they win over the face's own
 * stylesheet, which the face puts into the page after it.
 */
final case class Skin(look: Look, css: String)

object Skin:
  /** the stylesheet's rules, in the tokens only: one text for every accent and scheme */
  private val rules: String =
    """html, body { background: var(--okay-bg); margin: 0; min-height: 100%; }
      |body { display: flex; flex-direction: column; min-height: 100vh; }
      |body > #app { flex: 1; }
      |
      |/* the page's frame: a bar with the application's name, a line at the foot */
      |body > header { background: var(--ui-deep); color: #ffffff; }
      |body > header .bar, body > footer .bar { max-width: var(--okay-measure); margin: 0 auto; padding: 14px 16px;
      |  box-sizing: border-box; font: 15px/1.4 var(--okay-font); display: flex; align-items: baseline; gap: 12px; }
      |body > header .brand { font-weight: 800; font-size: 18px; color: #ffffff; text-decoration: none; }
      |body > header .tag { color: rgba(255, 255, 255, 0.7); }
      |body > footer { border-top: 1px solid var(--okay-border); color: var(--okay-muted); }
      |body > footer .bar { padding: 18px 16px 32px; font-size: 14px; display: block; }
      |body > footer a { color: var(--okay-accent); font-weight: 600; }
      |
      |/* the screen */
      |body .okay-face { padding: 32px 16px 48px; gap: 20px; }
      |body .okay-face > div { display: flex; flex-direction: column; gap: 20px; }
      |body .okay-heading-1 { font-size: 2em; letter-spacing: -0.02em; line-height: 1.2; }
      |body .okay-heading-1 + .okay-text { color: var(--okay-muted); font-size: 1.1em; }
      |body .okay-group { gap: 12px; }
      |body .okay-group-title { text-transform: uppercase; letter-spacing: 0.08em; font-size: 0.78em;
      |  margin-top: 12px; }
      |body .okay-link, body .okay-ref { color: var(--okay-accent); font-weight: 600; }
      |
      |/* actions: filled for going on, outlined for going back */
      |body .okay-actions { gap: 12px; }
      |body .okay-action { border-radius: 10px; padding: 11px 20px; transition: filter 0.12s, transform 0.06s;
      |  box-shadow: 0 1px 2px rgba(20, 24, 40, 0.12); color: #ffffff; }
      |body .okay-action:hover { filter: brightness(1.1); }
      |body .okay-action:active { transform: translateY(1px); }
      |body .okay-action:focus-visible, body .okay-form input:focus, body .okay-form select:focus,
      |body .okay-form textarea:focus {
      |  outline: 3px solid color-mix(in srgb, var(--okay-accent) 30%, transparent); outline-offset: 1px; }
      |body .okay-action:is([data-action="back"], [data-action="cancel"], [data-action="ok"]) {
      |  background: var(--okay-surface); color: var(--okay-fg); border: 1px solid var(--okay-border); }
      |body .okay-action:is([data-action="back"], [data-action="cancel"], [data-action="ok"]):hover {
      |  border-color: var(--okay-accent); filter: none; }
      |
      |/* forms: a card, the fields two by two on a wide screen */
      |body .okay-actions > .okay-form { flex: 1 1 100%; }
      |body .okay-form { background: var(--okay-surface); border: 1px solid var(--okay-border); border-radius: 14px;
      |  box-shadow: var(--ui-shadow); padding: 24px; gap: 18px; }
      |body .okay-form fieldset { border: 0; margin: 0; padding: 0; display: grid;
      |  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px 20px; }
      |body .okay-field { align-items: stretch; gap: 6px; }
      |body .okay-label { font-size: 0.8em; color: var(--okay-muted); text-transform: uppercase;
      |  letter-spacing: 0.06em; }
      |body .okay-form input, body .okay-form select, body .okay-form textarea { font: 16px/1.3 var(--okay-font);
      |  padding: 10px 12px; width: 100%;
      |  box-sizing: border-box; border: 1px solid var(--okay-border); border-radius: 10px; background: var(--okay-bg);
      |  color: var(--okay-fg); }
      |body .okay-form input:focus, body .okay-form select:focus, body .okay-form textarea:focus {
      |  border-color: var(--okay-accent); }
      |body .okay-form textarea { min-height: 88px; resize: vertical; }
      |body .okay-form > .okay-action { align-self: flex-start; min-width: 160px; }
      |/* the fields held back: Options, a row of its own at the end, its fields as the others when opened */
      |body .okay-form fieldset > .okay-more { grid-column: 1 / -1; }
      |body .okay-more > summary { cursor: pointer; width: fit-content; color: var(--okay-accent); font-weight: 600;
      |  font-size: 0.9em; }
      |body .okay-more > .okay-field { margin-top: 14px; }
      |body .okay-more::details-content { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      |  column-gap: 20px; }
      |body .okay-doc { font-size: 0.85em; }
      |body .okay-problems { font-weight: 600; }
      |
      |/* tables and notices: cards */
      |body .okay-table { background: var(--okay-surface); border-collapse: separate; border-spacing: 0; width: 100%;
      |  border: 1px solid var(--okay-border); border-radius: 12px; overflow: hidden; box-shadow: var(--ui-shadow); }
      |body .okay-table th { background: var(--ui-soft); text-align: left; font-size: 0.8em;
      |  text-transform: uppercase;
      |  letter-spacing: 0.05em; color: var(--okay-muted); }
      |body .okay-table th, body .okay-table td { border: 0; border-bottom: 1px solid var(--okay-border);
      |  padding: 11px 14px; }
      |body .okay-table tbody tr:last-child td { border-bottom: 0; }
      |body .okay-table tbody tr:hover td { background: color-mix(in srgb, var(--okay-accent) 5%, transparent); }
      |body .okay-table td { overflow-wrap: break-word; }
      |body .okay-notice-info, body .okay-told, body .okay-datum { border-radius: 10px;
      |  border: 1px solid var(--okay-border); }
      |body .okay-notice-warning, body .okay-notice-problem { border-radius: 10px; }
      |
      |@media (max-width: 640px) {
      |  body .okay-face { padding: 20px 16px 36px; }
      |  body .okay-heading-1 { font-size: 1.6em; }
      |  body .okay-actions > .okay-action { flex: 1 1 auto; }
      |}
      |@media (prefers-reduced-motion: reduce) { body .okay-action { transition: none; } }
      |
      |/* buttons at a slot (button.at): in a row where the screen puts them, drawn as buttons */
      |body .okay-mark-slot > .okay-group { display: flex; flex-direction: row; flex-wrap: wrap; gap: 8px; }
      |body .okay-mark-button > .okay-ref { display: inline-flex; align-items: center; justify-content: center;
      |  min-height: 40px; padding: 8px 16px; border-radius: 10px; text-decoration: none; font-weight: 600;
      |  border: 1px solid var(--okay-border); background: var(--okay-surface); color: var(--okay-fg); }
      |body .okay-mark-button-primary > .okay-mark-button > .okay-ref { background: var(--okay-accent);
      |  border-color: var(--okay-accent); color: #ffffff; }
      |body .okay-mark-button-danger > .okay-mark-button > .okay-ref { color: var(--okay-problem); }
      |/* a tree (Part.tree): nodes open and close by a click on their label; closed, their children hidden */
      |body .okay-mark-tree { display: block; }
      |body .okay-mark-tree > .okay-group { font-family: var(--okay-mono); font-size: 0.85em; line-height: 1.5;
      |  background: var(--okay-surface); border: 1px solid var(--okay-border); border-radius: 10px;
      |  padding: 10px 14px; overflow-x: auto; }
      |body .okay-mark-tree .okay-mark, body .okay-mark-tree .okay-group { display: block; margin: 0; padding: 0;
      |  border: 0; background: none; box-shadow: none; }
      |body .okay-mark-tree .okay-text { margin: 0; white-space: pre; font-family: inherit; }
      |body .okay-mark-tree-children { padding-left: 1.4em !important; border-left: 1px dotted var(--okay-border); }
      |body .okay-mark-tree-node > .okay-mark > .okay-group > .okay-mark-tree-children,
      |body .okay-mark-tree-node-current > .okay-mark > .okay-group > .okay-mark-tree-children { display: none; }
      |body .okay-mark-tree-label > .okay-text { cursor: pointer; }
      |body .okay-mark-tree-label > .okay-text::before { content: "▸ "; color: var(--okay-muted); }
      |body .okay-mark-tree-node-open > .okay-mark > .okay-group > .okay-mark-tree-label > .okay-text::before,
      |body .okay-mark-tree-node-open-current > .okay-mark > .okay-group > .okay-mark-tree-label > .okay-text::before {
      |  content: "▾ "; }
      |body .okay-mark-tree-leaf > .okay-text::before { content: "  "; }
      |body .okay-mark-tree-node-current > .okay-mark > .okay-group > .okay-mark > .okay-text,
      |body .okay-mark-tree-node-open-current > .okay-mark > .okay-group > .okay-mark > .okay-text {
      |  background: color-mix(in srgb, var(--okay-accent) 16%, transparent);
      |  box-shadow: inset 3px 0 0 var(--okay-accent); }
      |/* a file field (Field.file): its chooser under the text area, the area a drop target */
      |body label.okay-file { display: inline-flex; align-items: center; gap: 8px; margin-top: 6px;
      |  color: var(--okay-accent); font-weight: 600; cursor: pointer; }
      |body label.okay-file input[type="file"] { font: inherit; color: var(--okay-muted); }
      |/* text as written (Part.code): lines kept, monospace, the current ones marked */
      |body .okay-mark-code { display: block; }
      |body .okay-mark-code > .okay-group { font-family: var(--okay-mono); font-size: 0.85em; line-height: 1.45;
      |  background: var(--okay-surface); border: 1px solid var(--okay-border); border-radius: 10px;
      |  padding: 10px 0; overflow-x: auto; }
      |body .okay-mark-code .okay-mark-code-line, body .okay-mark-code .okay-mark-code-line-current { display: block; }
      |body .okay-mark-code .okay-text { margin: 0; padding: 0 14px; white-space: pre; font-family: inherit; }
      |body .okay-mark-code-line-current > .okay-text { background: color-mix(in srgb, var(--okay-accent) 16%,
      |  transparent); box-shadow: inset 3px 0 0 var(--okay-accent); }
      |body .okay-form input[type="checkbox"] { width: 22px; height: 22px; padding: 0; align-self: flex-start;
      |  margin: 8px 0;
      |  accent-color: var(--okay-accent); }
      |""".stripMargin

  /** okay-ui's look: forms and tables as cards, the fields side by side on a wide screen, the actions that go back
   * outlined; `accent` for buttons and links, `deep` for the page's bar, light or dark by `scheme` */
  def calm(accent: String = "#3b5bdb", deep: String = "#1e2a5a", scheme: Scheme = Scheme.Auto): Skin =
    val a = Color(accent).hex
    val d = Color(deep).hex
    val light = "--okay-fg: #1b2030; --okay-bg: #f4f5f9; --okay-muted: #5e6578; --okay-surface: #ffffff; " +
      "--okay-border: #dcdfe8; --okay-warning: #9a6700; --okay-problem: #c92a2a; " +
      "--ui-shadow: 0 1px 2px rgba(20, 24, 40, 0.06), 0 8px 24px rgba(20, 24, 40, 0.06);"
    val dark = "--okay-fg: #e6e8ef; --okay-bg: #12141b; --okay-muted: #9aa1b2; --okay-surface: #1b1e28; " +
      "--okay-border: #2e3342; --okay-warning: #e3b341; --okay-problem: #ff6b6b; " +
      "--ui-shadow: 0 1px 2px rgba(0, 0, 0, 0.3), 0 8px 24px rgba(0, 0, 0, 0.25);"
    val tokens = s"""|:root:root {
      |  --ui-accent: $a; --ui-deep: $d; --okay-accent: var(--ui-accent);
      |  --ui-soft: color-mix(in srgb, var(--ui-accent) 10%, var(--okay-surface));
      |  --okay-font: "Inter", system-ui, -apple-system, "Segoe UI", sans-serif;
      |  --okay-mono: ui-monospace, "SF Mono", Menlo, monospace;
      |  --okay-size: 16px; --okay-space: 8px; --okay-measure: 880px;
      |  ${if scheme == Scheme.Dark then dark else light}
      |}
      |""".stripMargin
    val auto = if scheme == Scheme.Auto then s"@media (prefers-color-scheme: dark) { :root:root { $dark } }\n" else ""
    val theme = Theme.light.copy(palette = Theme.light.palette.copy(accent = Color(a)))
    Skin(Look.default.copy(theme = theme), tokens + auto + rules)

  /** okay-faces' own look alone: no stylesheet of okay-ui's */
  val plain: Skin = Skin(Look.default, "")

  /** what a page wears when nothing is said */
  val default: Skin = calm()
