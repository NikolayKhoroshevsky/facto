package models.user

import models.Entity
import models.manager.EntityManager
import scala.collection.immutable.Seq

case class User(loginName: String,
                passwordHash: String,
                name: String,
                databaseEncryptionKey: String,
                expandCashFlowTablesByDefault: Boolean,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[User] {
    def findByIdSync(id: Long): User
    def fetchAllSync(): Seq[User]
    def findByLoginName(loginName: String): Option[User]
  }
}
