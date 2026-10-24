# Releasing

Releases are a Maven repository on GitHub Pages: the `gh-pages` branch, its `maven/` directory (beside the live demo),
served at https://okay-effects.github.io/okay-ui/maven. No account and no token is needed to use it, only resolvers:
this one and those of what it depends on.

## Using a release

```scala-sbt
resolvers ++= Seq("okay-effects" at "https://okay-effects.github.io/okay-simple/maven",
  "okay-scala" at "https://okay-effects.github.io/okay-scala/maven",
  "okay-faces" at "https://okay-effects.github.io/okay-faces/maven",
  "okay-http" at "https://okay-effects.github.io/okay-http/maven",
  "okay-ui" at "https://okay-effects.github.io/okay-ui/maven")

libraryDependencies ++= Seq(
  "io.github.okay-effects" %% "okay-ui-scala3" % "0.1.0",   // the program: okay.business.scala3.*
  "io.github.okay-effects" %% "okay-ui-faces"  % "0.1.0",   // its screens on okay-faces, Skin
  "io.github.okay-effects" %% "okay-ui"        % "0.1.0")   // with Scala.js (%%%): Laminar.run, Page
```

Scala 3.3 LTS, for the JVM and Scala.js.

## Making one

While nobody but us uses it, a change is REPUBLISHED under the current number (0.1.0), the tag moved; a new number
only when someone else depends on it.

1. Everything green with no local copies of the libraries:
   `sbt -Dsbt.ivy.home=$(mktemp -d) -Dplatforms=jvm,js clean test`.
2. `ThisBuild / version` without `-SNAPSHOT`; committed, tagged (`git tag -f v0.1.0`).
3. `rm -rf target/pages-maven && sbt -Dplatforms=jvm,js publish`.
4. Onto `gh-pages`, under `maven/`:

   ```sh
   git worktree add /tmp/okay-ui-pages gh-pages
   mkdir -p /tmp/okay-ui-pages/maven && rsync -ac target/pages-maven/ /tmp/okay-ui-pages/maven/   # -c: a republished checksum keeps its size
   git -C /tmp/okay-ui-pages add -A && git -C /tmp/okay-ui-pages commit -m "release 0.1.0"
   git -C /tmp/okay-ui-pages push origin gh-pages && git worktree remove /tmp/okay-ui-pages
   git push -f origin v0.1.0
   ```
