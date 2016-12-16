package common.time

import java.time.{LocalDate, LocalTime, Month}
import common.Require.requireNonNull

/**
  * Extension of `LocalDateTime`, which should keep the same API as `java.time.LocalDateTime`.
  */
object LocalDateTimes {

  def ofJavaLocalDateTime(javaDateTime: java.time.LocalDateTime): LocalDateTime = {
    LocalDateTime.of(javaDateTime.toLocalDate, javaDateTime.toLocalTime)
  }

  def of(year: Int, month: Month, dayOfMonth: Int): LocalDateTime = {
    LocalDateTime.of(year, month, dayOfMonth, 0, 0)
  }
}