package okay.business.reference

import okay.business.scala3.Day
import scala.scalajs.js

/** the page's clock: `js.Date`, here alone */
trait TodayOnPlatform:
  val system: Handles[Today] = Clock { () =>
    val d = new js.Date()
    Day(d.getFullYear().toInt, d.getMonth().toInt + 1, d.getDate().toInt)
  }
