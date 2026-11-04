package okay.business.laminar

import okay.business.faces.OnFaces
import okay.business.model.{Bundle, Screen}
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

  /** MANY FILES (spec, "Many files"): after each many-file field's area "Choose a folder…" and "Choose files…", the
   * area a drop target for several; each choice added to what the area holds, as a bundle */
  def attachBundles(root: dom.Element, screen: Screen[?]): Unit =
    OnFaces.bundles(screen).foreach { (name, accept) =>
      Option(root.querySelector(s"textarea#f-$name")).foreach { area =>
        val textArea = area.asInstanceOf[dom.HTMLTextAreaElement]
        val next = area.nextElementSibling
        if next == null || !next.classList.contains(mark) then
          def chooser(words: String, folder: Boolean): dom.HTMLLabelElement =
            val label = dom.document.createElement("label").asInstanceOf[dom.HTMLLabelElement]
            label.className = mark
            val input = dom.document.createElement("input").asInstanceOf[dom.HTMLInputElement]
            input.`type` = "file"
            input.multiple = true
            if folder then input.setAttribute("webkitdirectory", "")
            else if accept.nonEmpty then input.accept = accept
            input.addEventListener("change", (_: dom.Event) =>
              addAll((0 until input.files.length).map(input.files(_)).toList, accept, textArea))
            label.appendChild(input)
            label.appendChild(dom.document.createTextNode(words))
            label
          val files = chooser("Choose files…", folder = false)
          area.parentNode.insertBefore(files, area.nextSibling)
          area.parentNode.insertBefore(chooser("Choose a folder…", folder = true), area.nextSibling)
          area.addEventListener("dragover", (e: dom.DragEvent) => e.preventDefault())
          area.addEventListener("drop", (e: dom.DragEvent) =>
            if e.dataTransfer != null && e.dataTransfer.files.length > 0 then
              e.preventDefault()
              addAll((0 until e.dataTransfer.files.length).map(e.dataTransfer.files(_)).toList, accept, textArea))
      }
    }

  /** a file's path: under the folder chosen, or its name */
  def pathOf(file: dom.File): String =
    val relative = file.asInstanceOf[js.Dynamic].webkitRelativePath
    val path = if js.isUndefined(relative) || relative == null then "" else relative.asInstanceOf[String]
    if path.isEmpty then file.name else path

  /** the endings `accept` names (".log,.txt"), a file kept when it ends with one; none named, all kept */
  def accepted(path: String, accept: String): Boolean =
    val ends = accept.split(",").map(_.trim.toLowerCase).filter(_.startsWith(".")).toList
    ends.isEmpty || ends.exists(path.toLowerCase.endsWith)

  /** at most this many bytes a file: a folder may hold what is not text */
  val largest: Double = 20e6

  /** `files` read, those accepted, and added to `area` as a bundle once all are read */
  def addAll(files: List[dom.File], accept: String, area: dom.HTMLTextAreaElement): Unit =
    val kept = files.filter(f => accepted(pathOf(f), accept) && f.size <= largest).sortBy(pathOf)
    val texts = scala.collection.mutable.Map.empty[String, String]
    kept.foreach { f =>
      val reader = new dom.FileReader()
      reader.onload = (_: dom.ProgressEvent) =>
        texts(pathOf(f)) = reader.result.asInstanceOf[String]
        if texts.size == kept.size then add(area, kept.map(k => pathOf(k) -> texts(pathOf(k))))
      reader.readAsText(f)
    }

  /** `files` added to what `area` holds, as a bundle */
  def add(area: dom.HTMLTextAreaElement, files: List[(String, String)]): Unit =
    val before = area.value
    fill(area, (if before.trim.isEmpty then "" else before.stripSuffix("\n") + "\n\n") + Bundle.write(files))

  /** now, and once more when the page has drawn what it had to: a face may draw after the call that asked it */
  def attachSoon(root: dom.Element, screen: Screen[?]): Unit =
    attach(root, screen)
    js.timers.setTimeout(0)(attach(root, screen))
    attachBundles(root, screen)
    js.timers.setTimeout(0)(attachBundles(root, screen))

  /** `file`'s text into `area`, as if typed: the face reads the area when the form is sent */
  def read(file: dom.Blob, area: dom.HTMLTextAreaElement): Unit =
    val reader = new dom.FileReader()
    reader.onload = (_: dom.ProgressEvent) => fill(area, reader.result.asInstanceOf[String])
    reader.readAsText(file)

  def fill(area: dom.HTMLTextAreaElement, text: String): Unit =
    area.value = text
    area.dispatchEvent(new dom.Event("input", js.Dynamic.literal(bubbles = true).asInstanceOf[dom.EventInit]))
