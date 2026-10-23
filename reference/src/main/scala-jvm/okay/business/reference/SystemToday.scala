package okay.business.reference

import okay.business.scala3.Day

/** the JVM's clock: `java.time`, here alone */
trait TodayOnPlatform:
  val system: Handles[Today] = Clock { () =>
    val d = java.time.LocalDate.now()
    Day(d.getYear, d.getMonthValue, d.getDayOfMonth)
  }
