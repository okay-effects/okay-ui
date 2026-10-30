package okay.business.laminar

import okay.business.faces.OnFaces
import okay.business.model.Screen
import org.scalajs.dom
import scala.scalajs.js

/**
 * A FILE'S TEXT INTO ITS FIELD, in a page: beside each file field's text area (`Field.file`, drawn by the face as long
 * text) a chooser — "Choose a file…" — and the area itself a drop target; the file's text is read into the area, where
 * the face reads it when the form is sent. Every face of okay-faces keeps its markup; this only adds to a page's.
 */
object FilePicking:
  /** the mark a chooser carries, so a screen drawn again is not given a second one */
  val mark = "okay-file"

  /** after `screen` is drawn into `root`: a chooser and a drop for each of its file fields */
  def attach(root: dom.Element, screen: Screen[?]): Unit =
    OnFaces.files(screen).foreach { (name, accept) =>
      Option(root.querySelector(s"textarea#f-$name")).foreach { area =>
        val textArea = area.asInstanceOf[dom.HTMLTextAreaElement]
        val next = area.nextElementSibling
        if next == null || !next.classList.contains(mark) then
          val label = dom.document.createElement("label").asInstanceOf[dom.HTMLLabelElement]
          label.className = mark
          val input = dom.document.createElement("input").asInstanceOf[dom.HTMLInputElement]
          input.`type` = "file"
          if accept.nonEmpty then input.accept = accept
          input.addEventListener("change", (_: dom.Event) =>
            if input.files.length > 0 then read(input.files(0), textArea))
          label.appendChild(input)
          label.appendChild(dom.document.createTextNode("Choose a file…"))
          area.parentNode.insertBefore(label, area.nextSibling)
          area.addEventListener("dragover", (e: dom.DragEvent) => e.preventDefault())
          area.addEventListener("drop", (e: dom.DragEvent) =>
            if e.dataTransfer != null && e.dataTransfer.files.length > 0 then
              e.preventDefault()
              read(e.dataTransfer.files(0), textArea))
      }
    }

  /** now, and once more when the page has drawn what it had to: a face may draw after the call that asked it */
  def attachSoon(root: dom.Element, screen: Screen[?]): Unit =
    attach(root, screen)
    js.timers.setTimeout(0)(attach(root, screen))

  /** `file`'s text into `area`, as if typed: the face reads the area when the form is sent */
  def read(file: dom.Blob, area: dom.HTMLTextAreaElement): Unit =
    val reader = new dom.FileReader()
    reader.onload = (_: dom.ProgressEvent) => fill(area, reader.result.asInstanceOf[String])
    reader.readAsText(file)

  def fill(area: dom.HTMLTextAreaElement, text: String): Unit =
    area.value = text
    area.dispatchEvent(new dom.Event("input", js.Dynamic.literal(bubbles = true).asInstanceOf[dom.EventInit]))
