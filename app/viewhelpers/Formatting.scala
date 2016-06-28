package viewhelpers

import java.lang.Math.abs

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat.forPattern
import com.github.nscala_time.time.Imports._
import play.api.i18n.Messages

import common.Clock

object Formatting {

  def formatDate(date: DateTime)
                (implicit messages: Messages) = {
    val now = Clock.now

    val yearString = date.toString(forPattern("yy"))
    val dayMonthString = date.toString(forPattern("d MMM"))
    val dayOfWeek = date.toString(forPattern("EEE"))

    if (date.getYear == now.getYear) {
      val dayDifference = abs(now.getDayOfYear - date.getDayOfYear)

      if (date.getDayOfYear == now.getDayOfYear) {
        Messages("facto.today")
      } else if (date.getDayOfYear == now.getDayOfYear - 1) {
        Messages("facto.yesterday")
      } else if (date.getDayOfYear == now.getDayOfYear + 1) {
        Messages("facto.tomorrow")
      } else if (dayDifference < 7) {
        s"**$dayOfWeek, $dayMonthString"
      } else {
        "**" + dayMonthString
      }
    } else {
      s"**$dayMonthString '$yearString"
    }
  }

  def formatDateTime(date: DateTime) = {
    date.toString(forPattern("d MMM yyyy, HH:mm"))
  }
}
