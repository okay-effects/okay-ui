package okay.business.laminar

import okay.business.faces.OnFaces
import okay.business.model.{Button, Field, Form, Screen, Ui}
import okay.business.scala3.show
import okay.scala3.*
import okay.scala3.std.Async
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** A FILE FIELD IN A PAGE, under jsdom: the face's text area, a chooser beside it once, the file's text read into it */
class FilePickingSuite extends munit.FunSuite:
  val form = Form(Field.file("Transaction", ".json,.signed"), Field.text("Note").optional)
  val screen = Screen[(String, Option[String])]("Files")(form)(Button.submit("Read", form)(identity))

  test("the screen's file fields, as the face names them"):
    assertEquals(OnFaces.files(screen), List("transaction" -> ".json,.signed"))

  test("drawn: a chooser after the file field's area, once however often it is attached; its text filled in"):
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    type Page[X] = (Async + Ui)[X]
    val running = Async.start(Laminar.run(root)(show[(String, Option[String]), Page](screen)))
    FilePicking.attach(root, screen)
    val area = root.querySelector("textarea#f-transaction").asInstanceOf[dom.HTMLTextAreaElement]
    assert(area != null, root.innerHTML)
    assertEquals(root.querySelectorAll("label.okay-file").length, 1)
    assertEquals(root.querySelector("label.okay-file input").getAttribute("accept"), ".json,.signed")
    FilePicking.fill(area, """{"cborHex": "84a4"}""")
    assertEquals(area.value, """{"cborHex": "84a4"}""")
    running.cancel()

  test("many files (spec, \"Many files\"): a folder chooser and a files chooser, once; each choice added as a bundle"):
    val many = Form.one(Field.files("Folder", ".log,.txt"))
    val s = Screen[List[(String, String)]]("Folders")(many)(Button.submit("Read", many)(identity))
    assertEquals(OnFaces.bundles(s), List("folder" -> ".log,.txt"))
    dom.document.body.innerHTML = ""
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    type Page[X] = (Async + Ui)[X]
    val running = Async.start(Laminar.run(root)(show[List[(String, String)], Page](s)))
    FilePicking.attachBundles(root, s)
    FilePicking.attachBundles(root, s)
    val labels = root.querySelectorAll("label.okay-file")
    assertEquals(labels.length, 2)
    assert(root.querySelector("label.okay-file input[webkitdirectory]") != null, root.innerHTML)
    val area = root.querySelector("textarea#f-folder").asInstanceOf[dom.HTMLTextAreaElement]
    FilePicking.add(area, List("a/web.log" -> "x\n"))
    FilePicking.add(area, List("b/db.log" -> "y\n"))
    assertEquals(okay.business.model.Bundle.read(area.value), List("a/web.log" -> "x\n", "b/db.log" -> "y\n"))
    assert(FilePicking.accepted("logs/web.LOG", ".log,.txt") && !FilePicking.accepted("x.png", ".log,.txt"))
    running.cancel()
