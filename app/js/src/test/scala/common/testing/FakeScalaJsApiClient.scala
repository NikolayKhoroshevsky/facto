package common.testing

import api.ScalaJsApi.{GetAllEntitiesResponse, GetEntityModificationsResponse, UpdateToken}
import api.ScalaJsApiClient
import models.Entity
import models.modification.EntityType
import models.modification.EntityModification

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class FakeScalaJsApiClient extends ScalaJsApiClient {

  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()

  // **************** Implementation of ScalaJsApiClient trait ****************//
  override def getInitialData() = {
    ???
  }

  override def getAllEntities(types: Seq[EntityType.any]) = Future.successful {
    GetAllEntitiesResponse(
      entitiesMap = {
        for (entityType <- types) yield {
          entityType -> modificationsBuffer.getAllEntitiesOfType(entityType)
        }
      }.toMap,
      nextUpdateToken = modificationsBuffer.nextUpdateToken
    )
  }

  override def getEntityModifications(updateToken: UpdateToken) = Future.successful {
    GetEntityModificationsResponse(
      modifications = modificationsBuffer.getModifications(updateToken),
      nextUpdateToken = modificationsBuffer.nextUpdateToken)
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = {
    modificationsBuffer.addModifications(modifications)
    Future.successful((): Unit)
  }

  // **************** Additional methods for tests ****************//
  def addEntities[E <: Entity: EntityType](entities: E*): Unit = {
    modificationsBuffer.addEntities(entities.toVector)
  }

  def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()
}