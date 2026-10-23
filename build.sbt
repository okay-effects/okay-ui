// okay-ui: business applications' screens as data, for Scala 3 alone — package okay.business
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scala3 = "3.3.8"
val okayScala = "0.1.0"
val okayEffects = "0.1.1"
/** okay-simple's interface on okay-scala's machine: not released yet — `publishLocal` from okay-simple's master */
val okayEffectsCek = "0.1.2-SNAPSHOT"

ThisBuild / organization := "io.github.okay-effects"
ThisBuild / homepage := Some(url("https://github.com/okay-effects/okay-ui"))
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := scala3
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:imports,privates,locals")
ThisBuild / resolvers += "okay-effects" at "https://okay-effects.github.io/okay-simple/maven"
ThisBuild / resolvers += "okay-scala" at "https://okay-effects.github.io/okay-scala/maven"
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

val platformsAsked: Set[String] =
  sys.props.get("platforms").getOrElse("jvm").split(',').map(_.trim.toLowerCase).toSet + "jvm"
// a module's Scala.js settings are `.whenJs(...)` (project/WhenJs.scala): set only when Scala.js is asked
val platforms: Seq[sbtcrossproject.Platform] =
  Seq[sbtcrossproject.Platform](JVMPlatform) ++ (if (platformsAsked("js")) Seq(JSPlatform) else Nil)
val munit = Def.setting("org.scalameta" %%% "munit" % "1.2.3" % Test)

/** THE MODEL, THE EFFECT, THE KERNEL'S WORDS: no dependency */
lazy val core = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("core"))
  .settings(name := "okay-ui-core", libraryDependencies += munit.value)

/** on okay-scala's machine: `import okay.business.scala3.*` */
lazy val scala3Kernel = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("scala3"))
  .dependsOn(core)
  .settings(name := "okay-ui-scala3", libraryDependencies += "io.github.okay-effects" %%% "okay-scala" % okayScala)

/** on okay-simple's machine: `import okay.business.simple.*` */
lazy val simpleKernel = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("simple"))
  .dependsOn(core)
  .settings(name := "okay-ui-simple",
    libraryDependencies ++= Seq("io.github.okay-effects" %%% "okay-effects-machine" % okayEffects,
      "io.github.okay-effects" %%% "okay-effects-free" % okayEffects))

/** okay-faces with `View.Link`: not released yet — `publishLocal` from okay-faces' master */
val okayFaces = "0.1.1-SNAPSHOT"
ThisBuild / resolvers += "okay-faces" at "https://okay-effects.github.io/okay-faces/maven"
ThisBuild / resolvers += "okay-http" at "https://okay-effects.github.io/okay-http/maven"

/** THE SCREENS ON OKAY-FACES: every face of it — terminal, HTML, Swing, Laminar, MCP — runs a business program; on
 * okay-scala's machine, through okay-simple's adapter: `import okay.effects.cek.OnCek.self` */
lazy val faces = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("faces"))
  .dependsOn(core)
  .settings(name := "okay-ui-faces",
    libraryDependencies ++= Seq("io.github.okay-effects" %%% "okay-faces" % okayFaces,
      "io.github.okay-effects" %%% "okay-effects-cek" % okayEffectsCek,
      "io.github.okay-effects" %%% "okay-faces-test" % okayFaces % Test, munit.value))

/** THE EXAMPLE, written once on okay-scala; its copies on okay-simple's machine and Free made by changing the import
 * line (and the package) alone — the proof that the kernel is the import. The shop and its scripted tests shared; at a
 * terminal on the JVM (`scala-jvm`), in a page with Scala.js (`scala-js`: `sbt -Dplatforms=js examplesJS/web`) */
val web = taskKey[File]("a page: index.html and its script, linked, in target/web")
/** the module's `web/index.html` and its linked script, `main.js`, side by side in target/web */
def page(what: String) = Def.task {
  val linked = (Compile / fastLinkJS).value.data.publicModules.head.jsFileName
  val from = (Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value / linked
  val out = target.value / "web"
  IO.copyFile(baseDirectory.value.getParentFile / "web" / "index.html", out / "index.html")
  IO.copyFile(from, out / "main.js")
  streams.value.log.info(s"$what: ${out / "index.html"}")
  out
}
/** the page tested under jsdom, clicked through as a person would; jsdom from NODE_PATH */
val jsdom = Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(
  org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv.Config()
    .withEnv(Map("NODE_PATH" -> sys.env.getOrElse("NODE_PATH", "/opt/homebrew/lib/node_modules"))))
lazy val examples = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("examples"))
  .dependsOn(scala3Kernel, simpleKernel, faces, ui)
  .settings(name := "okay-ui-examples", publish / skip := true,
    libraryDependencies ++= Seq(munit.value, "io.github.okay-effects" %%% "okay-faces-test" % okayFaces % Test),
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala",
    Compile / sourceGenerators += Def.task {
      val src = baseDirectory.value.getParentFile / "src" / "main" / "scala" / "okay" / "business" / "examples" /
        "Shop.scala"
      val text = IO.read(src)
      Seq("simple", "free").map { kernel =>
        val out = (Compile / sourceManaged).value / "okay" / "business" / "examples" / kernel / "Shop.scala"
        val copy = text.replace("package okay.business.examples.scala3", s"package okay.business.examples.$kernel")
          .replace("import okay.business.scala3.*", s"import okay.business.$kernel.*")
        require(copy.linesIterator.size == text.linesIterator.size)
        IO.write(out, copy)
        out
      }
    }.taskValue)
  .jvmSettings(
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala-jvm",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala-jvm",
    libraryDependencies += "io.github.okay-effects" %%% "okay-faces-term" % okayFaces)
  .whenJs(
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala-js",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala-js",
    libraryDependencies += "io.github.okay-effects" %%% "okay-faces-laminar" % okayFaces,
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("okay.business.examples.InTheBrowser"),
    jsdom,
    web := page("the shop's page").value)

/** THE REFERENCE APPLICATION, Expenses: its data model effects (`Ledger`, `Today`, `Ui`), every implementation a
 * handler, each environment one line naming its handlers — at a terminal on the JVM, in a page with Scala.js
 * (`sbt -Dplatforms=jvm,js referenceJS/web`). See specs/reference.md */
lazy val reference = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("reference"))
  .dependsOn(scala3Kernel, faces, ui)
  .settings(name := "okay-ui-reference", publish / skip := true,
    libraryDependencies ++= Seq(munit.value, "io.github.okay-effects" %%% "okay-effects-json" % okayEffects),
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala")
  .jvmSettings(
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala-jvm",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala-jvm",
    libraryDependencies += "io.github.okay-effects" %%% "okay-faces-term" % okayFaces,
    Compile / mainClass := Some("okay.business.reference.AtTheTerminal"),
    run / fork := true, run / connectInput := true)
  .whenJs(
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala-js",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala-js",
    libraryDependencies += "io.github.okay-effects" %%% "okay-faces-laminar" % okayFaces,
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("okay.business.reference.InTheBrowser"),
    jsdom,
    web := page("the expenses' page").value)

/** THE DEFAULT: okay-scala's machine and okay-faces' faces — `"io.github.okay-effects" %% "okay-ui" % ...` */
lazy val ui = crossProject(platforms: _*).crossType(CrossType.Pure).in(file("ui"))
  .dependsOn(scala3Kernel, faces)
  .settings(name := "okay-ui", libraryDependencies += munit.value)
  // IN A PAGE: `Laminar.run(root)`, `Ui` into okay-scala's `Lifted[Future]` on okay-faces' Laminar face
  .whenJs(
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "main" / "scala-js",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scala-js",
    libraryDependencies += "io.github.okay-effects" %%% "okay-faces-laminar" % okayFaces,
    jsdom)

lazy val root = (project in file(".")).aggregate(Seq(core, scala3Kernel, simpleKernel, faces, ui, examples, reference)
  .flatMap(_.componentProjects).map(p => p: ProjectReference): _*)
  .settings(publish / skip := true)
