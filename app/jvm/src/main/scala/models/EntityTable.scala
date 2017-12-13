package models

import models.SlickUtils.dbApi._

// Based on active-slick (https://github.com/strongtyped/active-slick)

/** Table extension to be used with an Entity model. */
abstract class EntityTable[M <: Entity](tag: Tag, tableName: String, schemaName: Option[String] = None)(
    implicit val colType: BaseColumnType[Long])
    extends Table[M](tag, schemaName, tableName) {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
}