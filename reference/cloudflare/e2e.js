// EXPENSES ON CLOUDFLARE, end to end: `wrangler dev`; two expenses added by the pages; the Worker started again —
// every object's memory gone, its storage kept — and the expenses where they were; another session has none.
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
const open = async () => (await fetch(base + "/", { redirect: "manual" })).headers.get("location");
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
  await stop(); await start();
  const again = await text(page);
  check("after a restart: Lunch and Ticket", /Lunch/.test(again) && /Ticket/.test(again));
  const other = await open();
  await act(other, "all-expenses");
  check("another session: none of them", !/Lunch/.test(await text(other)));
  await stop();
  console.log(ok ? "\nEXPENSES ON CLOUDFLARE OK" : "\nEXPENSES ON CLOUDFLARE FAILED\n" + out.slice(-1500));
  process.exit(ok ? 0 : 1);
})().catch(async e => { console.log("error:", e, out.slice(-1500)); await stop(); process.exit(1); });
