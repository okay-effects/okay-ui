#!/bin/sh
# THE LIVE DEMO: the reference application (Expenses) and the shop, linked optimised by Scala.js, as static pages for
# GitHub Pages — https://okay-effects.github.io/okay-ui/expenses/ and /shop/.
#   sh tools/publish-demo.sh [out-dir]       (default target/demo; then copy it onto the gh-pages branch)
set -eu
cd "$(dirname "$0")/.."
out="${1:-target/demo}"
sbt -batch -Dplatforms=jvm,js referenceJS/fullLinkJS examplesJS/fullLinkJS
rm -rf "$out"; mkdir -p "$out/expenses" "$out/shop"
cp reference/web/index.html "$out/expenses/index.html"
cp "$(find reference/.js/target -path '*-opt/main.js' | head -1)" "$out/expenses/main.js"
cp examples/web/index.html "$out/shop/index.html"
cp "$(find examples/.js/target -path '*-opt/main.js' | head -1)" "$out/shop/main.js"
cp web/okay-ui.css "$out/expenses/okay-ui.css"; cp web/okay-ui.css "$out/shop/okay-ui.css"
cat > "$out/index.html" <<'HTML'
<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>okay-ui demos</title></head>
<body style="font-family: system-ui, sans-serif; max-width: 40rem; margin: 3rem auto; padding: 0 1rem">
<h1>okay-ui</h1>
<p>Business screens as data on algebraic effects, in Scala 3, running in the browser through Scala.js.</p>
<ul>
  <li><a href="expenses/">Expenses</a> — the reference application: the data model as effects, every implementation a
    handler, kept in this browser's storage.</li>
  <li><a href="shop/">Shop</a> — customers and orders, the same program on three effect kernels.</li>
</ul>
<p><a href="https://github.com/okay-effects/okay-ui">Source on GitHub</a></p>
</body></html>
HTML
touch "$out/.nojekyll"
du -sh "$out"/*/main.js
