package models.modification

import java.nio.ByteBuffer

import api.Picklers._
import boopickle.Default._
import common.time.LocalDateTime
import models.EntityTable
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.manager.{ImmutableEntityManager, SlickEntityManager}
import models.modification.SlickEntityModificationEntityManager.{EntityModificationEntities, tableName}

final class SlickEntityModificationEntityManager
    extends ImmutableEntityManager[EntityModificationEntity, EntityModificationEntities](
      SlickEntityManager.create[EntityModificationEntity, EntityModificationEntities](
        tag => new EntityModificationEntities(tag),
        tableName = tableName
      ))
    with EntityModificationEntity.Manager

object SlickEntityModificationEntityManager {
  private val tableName: String = "ENTITY_MODIFICATION_ENTITY"

  final class EntityModificationEntities(tag: SlickTag)
      extends EntityTable[EntityModificationEntity](tag, tableName) {
    def userId = column[Long]("userId")
    def change = column[EntityModification]("modification")
    def date = column[LocalDateTime]("date")

    override def * =
      (userId, change, date, id.?) <> (EntityModificationEntity.tupled, EntityModificationEntity.unapply)
  }

  implicit val entityModificationToBytesMapper: ColumnType[EntityModification] = {
    def toBytes(modification: EntityModification) = {
      val byteBuffer = Pickle.intoBytes(modification)

      val byteArray = new Array[Byte](byteBuffer.remaining)
      byteBuffer.get(byteArray)
      byteArray
    }
    def toEntityModification(bytes: Array[Byte]) = {
      val byteBuffer = ByteBuffer.wrap(bytes)
      Unpickle[EntityModification].fromBytes(byteBuffer)
    }
    MappedColumnType.base[EntityModification, Array[Byte]](toBytes, toEntityModification)
  }
}
