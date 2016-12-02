package models

import java.time.{LocalDateTime => JavaLocalDateTime}
import common.time.LocalDateTime
import java.time.{ZoneId, LocalDate, LocalTime}
import models.accounting.money.Money

import scala.concurrent.Await
import scala.concurrent.duration._
import slick.backend.DatabaseConfig
import slick.driver.JdbcDriver

object SlickUtils {

  // ********** db helpers ********** //
  val dbConfig: DatabaseConfig[JdbcDriver] = DatabaseConfig.forConfig("db.default.slick")
  val dbApi = dbConfig.driver.api

  import dbApi._

  val database = Database.forConfig("db.default")

  def dbRun[T](query: DBIO[T]): T = {
    Await.result(database.run(query), Duration.Inf)
  }

  def dbRun[T, C[T]](query: Query[_, T, C]): C[T] = dbRun(query.result)

  // ********** datetime helpers ********** //
  implicit val LocalDateTimeToSqlDateMapper = {
    val zone = ZoneId.of("Europe/Paris") // This is arbitrary. It just has to be the same in both directions
    val toSql = (localDateTime: LocalDateTime) => {
      val javaDate = JavaLocalDateTime.of(localDateTime.toLocalDate, localDateTime.toLocalTime)
      val instant = javaDate.atZone(zone).toInstant
      java.sql.Timestamp.from(instant)
    }
    val toLocalDateTime = (sqlTimestamp: java.sql.Timestamp) => {
      val javaDate = sqlTimestamp.toInstant.atZone(zone).toLocalDateTime
      LocalDateTime.ofJavaLocalDateTime(javaDate)
    }
    MappedColumnType.base[LocalDateTime, java.sql.Timestamp](toSql, toLocalDateTime)
  }
}
