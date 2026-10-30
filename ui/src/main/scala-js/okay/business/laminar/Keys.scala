package okay.business.laminar

import okay.business.model.Events
import org.scalajs.dom

/**
 * KEYS PRESSED IN THE PAGE, as events of the model: `screen.until(Keys.events(pick))` answers the screen with
 * `pick(key)` for a key it knows (`dom.KeyboardEvent.key`: "ArrowRight", "j", "Home"), as a button would. A key typed
 * into a field, or with Ctrl / Alt / Meta held, is the field's or the browser's, never an event.
 */
object Keys:
  def events[A](pick: String => Option[A]): Events[A] = Events { k =>
    val listener: scalajs.js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
      val typing = e.target match
        case el: dom.Element => Set("INPUT", "TEXTAREA", "SELECT")(el.tagName) || el.hasAttribute("contenteditable")
        case _ => false
      if !typing && !e.ctrlKey && !e.altKey && !e.metaKey then
        pick(e.key).foreach { a =>
          e.preventDefault()
          k(a)
        }
    dom.document.addEventListener("keydown", listener)
    () => dom.document.removeEventListener("keydown", listener)
  }
