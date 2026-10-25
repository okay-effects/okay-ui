// EXPENSES ON CLOUDFLARE, end to end: `wrangler dev`; two expenses added by the pages; one by an agent (MCP); the thin
// page in a headless Chrome told by its socket when its session changed elsewhere; the Worker started again — every
// object's memory gone, its storage kept — and the expenses where they were; another session has none.
// `node reference/cloudflare/e2e.js` after `sbt -Dplatforms=jvm,js referenceCloudflareJS/worker`
const { spawn } = require("child_process"); const path = require("path");
const sleep = ms => new Promise(r => setTimeout(r, ms));
const dir = path.join(__dirname, "worker"); const base = "http://127.0.0.1:8796";
let dev, out = "";
const start = async () => {
  out = ""; dev = spawn("npx", ["-y", "wrangler@4", "dev", "--port", "8796", "--ip", "127.0.0.1"], { cwd: dir, detached: true });
  dev.stdout.on("data", d => out += d); dev.stderr.on("data", d => out += d);
  for (let i = 0; i < 120 && !/Ready on/.test(out); i++) await sleep(500);
  await sleep(500);
};
const stop = async () => { try { process.kill(-dev.pid, "SIGTERM"); } catch (e) {} await sleep(1500); };
const act = (page, name, fields = {}) => fetch(base + page + "/act/" + name, { method: "POST", redirect: "manual",
  headers: { "content-type": "application/x-www-form-urlencoded" }, body: new URLSearchParams(fields).toString() });
const text = async page => (await (await fetch(base + page)).text()).replace(/<[^>]+>/g, " ");
const open = async () => (await fetch(base + "/s", { redirect: "manual" })).headers.get("location");
const add = async (page, amount, category, note) => {
  await act(page, "add-an-expense");
  const r = await act(page, "save", { amount, category, day: "2026-10-20", note });
  await act(page, "ok"); // "Saved …": OK
  return r.status;
};
(async () => {
  let ok = true; const check = (what, c) => { console.log(`${c ? "✓" : "✗"} ${what}`); ok = ok && !!c; };
  await start();
  const page = await open();
  check("a session: " + page, /^\/s\/[0-9a-f]{32}$/.test(page));
  check("lunch saved", (await add(page, "12.50", "Food", "Lunch")) === 303);
  check("a ticket saved", (await add(page, "3.20", "Transport", "Ticket")) === 303);
  await act(page, "all-expenses");
  const all = await text(page);
  check("all expenses: Lunch and Ticket", /Lunch/.test(all) && /Ticket/.test(all));
  // an agent: the same program as tools
  const mcp = async (body, sid) => {
    const r = await fetch(base + "/mcp", { method: "POST", headers: Object.assign({ "content-type": "application/json",
      accept: "application/json" }, sid ? { "mcp-session-id": sid } : {}), body: JSON.stringify(body) });
    return { sid: r.headers.get("mcp-session-id"), body: r.status === 202 ? null : await r.json() };
  };
  const init = await mcp({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-06-18",
    capabilities: {}, clientInfo: { name: "e2e" } } });
  check("MCP: a session " + init.sid, /^[0-9a-f]{32}$/.test(init.sid || ""));
  let id = 2; const call = async (name, args = {}) => JSON.stringify((await mcp({ jsonrpc: "2.0", id: id++,
    method: "tools/call", params: { name, arguments: args } }, init.sid)).body);
  const tools = (await mcp({ jsonrpc: "2.0", id: id++, method: "tools/list" }, init.sid)).body.result.tools.map(t => t.name);
  check("MCP: tools " + tools.join(","), tools.includes("add-an-expense"));
  await call("add-an-expense");
  const saved = await call("save", { amount: 7.5, category: "Fun", day: "2026-10-21", note: "Cinema" });
  check("MCP: saved — " + saved.slice(0, 120), /Saved/.test(saved));
  await call("ok");
  // the thin page: its session on the API, changed elsewhere, told by its socket
  const fs = require("fs"); fs.rmSync("/tmp/oue-chrome", { recursive: true, force: true });
  const chrome = spawn("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", ["--headless=new",
    "--remote-debugging-port=9373", "--user-data-dir=/tmp/oue-chrome", "about:blank"], { stdio: "ignore" });
  let target; for (let i = 0; i < 40 && !target; i++) { await sleep(250);
    try { target = (await (await fetch("http://127.0.0.1:9373/json")).json()).find(t => t.type === "page"); } catch (e) {} }
  const ws = new WebSocket(target.webSocketDebuggerUrl); let n = 0; const pending = {}; const events = [];
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pending[m.id]) { pending[m.id](m.result); delete pending[m.id]; }
    else if (m.method) events.push(m); };
  await new Promise(r => ws.onopen = r);
  const cdp = (method, params = {}) => new Promise(r => { const i = ++n; pending[i] = r; ws.send(JSON.stringify({ id: i, method, params })); });
  const js = async ex => (await cdp("Runtime.evaluate", { expression: ex, returnByValue: true })).result.value;
  await cdp("Network.enable");
  await cdp("Page.navigate", { url: base + "/" });
  for (let i = 0; i < 40 && !(await js("/Expenses/.test(document.body.innerText)")); i++) await sleep(250);
  check("the thin page drawn", await js("/Expenses/.test(document.body.innerText)"));
  const thin = events.find(e => e.method === "Network.responseReceived" && /\/sessions$/.test(e.params.response.url));
  const self = thin && (thin.params.response.headers.Location || thin.params.response.headers.location);
  for (let i = 0; i < 20 && !events.some(e => e.method === "Network.webSocketHandshakeResponseReceived"); i++) await sleep(250);
  check("its socket up", events.some(e => e.method === "Network.webSocketHandshakeResponseReceived"));
  await fetch(base + self + "/act/all-expenses", { method: "POST", headers: { "content-type": "application/json" }, body: "{}" });
  for (let i = 0; i < 20 && !(await js("/All expenses|no expenses/i.test(document.body.innerText)")); i++) await sleep(250);
  check("moved from elsewhere, the page follows", await js("/All expenses|no expenses/i.test(document.body.innerText)"));
  chrome.kill();
  await stop(); await start();
  const again = await text(page);
  check("after a restart: Lunch and Ticket", /Lunch/.test(again) && /Ticket/.test(again));
  const after = await call("all-expenses");
  check("after a restart, the agent's session: Cinema", /Cinema/.test(after));
  const other = await open();
  await act(other, "all-expenses");
  check("another session: none of them", !/Lunch/.test(await text(other)));
  await stop();
  console.log(ok ? "\nEXPENSES ON CLOUDFLARE OK" : "\nEXPENSES ON CLOUDFLARE FAILED\n" + out.slice(-1500));
  process.exit(ok ? 0 : 1);
})().catch(async e => { console.log("error:", e, out.slice(-1500)); await stop(); process.exit(1); });
