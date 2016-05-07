package models

import scala.concurrent.Await
import scala.concurrent.duration._

import slick.backend.DatabaseConfig
import slick.driver.JdbcDriver

import models.accounting.Money

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
  implicit val JodaToSqlDateMapper =
    MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.getMillis),
      d => new org.joda.time.DateTime(d.getTime))

  implicit val MoneyToLongMapper =
    MappedColumnType.base[Money, Long](
      money => money.cents,
      cents => Money(cents))
}