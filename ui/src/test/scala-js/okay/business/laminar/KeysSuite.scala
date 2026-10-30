package okay.business.laminar

import org.scalajs.dom
import scala.scalajs.js

/** KEYS AS EVENTS, under jsdom: a key known is an event, a key typed into a field or with a modifier is not */
class KeysSuite extends munit.FunSuite:
  def press(key: String, target: dom.EventTarget = dom.document, ctrl: Boolean = false): Unit =
    val init = js.Dynamic.literal(key = key, bubbles = true, ctrlKey = ctrl).asInstanceOf[dom.KeyboardEventInit]
    target.dispatchEvent(new dom.KeyboardEvent("keydown", init))

  test("a known key is an event until the listening stops"):
    var got = List.empty[Int]
    val stop = Keys.events(Map("ArrowRight" -> 1, "ArrowLeft" -> -1).get).register(n => got :+= n)
    press("ArrowRight")
    press("x")
    press("ArrowLeft")
    stop()
    press("ArrowRight")
    assertEquals(got, List(1, -1))

  test("typed into a field, or with Ctrl held, a key is not an event"):
    var got = List.empty[String]
    val input = dom.document.createElement("input")
    dom.document.body.appendChild(input)
    val stop = Keys.events(k => Some(k)).register(k => got :+= k)
    press("j", input)
    press("j", ctrl = true)
    press("k")
    stop()
    assertEquals(got, List("k"))
