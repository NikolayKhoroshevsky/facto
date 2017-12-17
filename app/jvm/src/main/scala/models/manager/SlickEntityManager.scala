package models.manager

import models.{Entity, EntityTable}
import models.SlickUtils.dbApi._
import slick.lifted.{AbstractTable, TableQuery}

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** Provides access to persisted entries using the Slick API. */
trait SlickEntityManager[E <: Entity, T <: AbstractTable[E]] extends EntityManager[E] {

  // ********** Management methods ********** //
  /** Creates the persisted database table for this manager. */
  def createTable(): Unit
  def tableName: String

  // ********** Mutators ********** //
  /** Persists a new entity (with ID) or does nothing if an entity with that ID already exists. */
  private[models] def addIfNew(entityWithId: E): Unit

  /** Updates an existing entity or does nothing if no entity with that ID exists. */
  private[models] def updateIfExists(entityWithId: E): Unit

  /** Deletes an existing entity or does nothing if no entity with that ID exists. */
  private[models] def deleteIfExists(entityId: Long): Unit

  // ********** Getters ********** //
  /**
    * Returns a new query that should be run by models.SlickUtils.dbRun. Don't run any mutating operations using these queries!
    *
    * Don't run any mutating operations using these queries!
    */
  def newQuery: TableQuery[T]

  /** Returns the entity with given ID or throws an exception. */
  final def findByIdSync(id: Long): E = Await.result(findById(id), Duration.Inf)

  /** Returns all stored entities. */
  final def fetchAllSync(): Seq[E] = Await.result(fetchAll(), Duration.Inf)
}

object SlickEntityManager {

  /** Factory method for creating a database backed SlickEntityManager. */
  def create[E <: Entity, T <: EntityTable[E]](cons: Tag => T, tableName: String): SlickEntityManager[E, T] = {
    new DatabaseBackedEntityManager[E, T](cons, tableName)
  }
}
