package models.access

import common.GuavaReplacement.ImmutableSetMultimap
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala2js.Converters._

case class PendingModifications(modifications: Seq[EntityModification], persistedLocally: Boolean) {
  private val addModificationIds: ImmutableSetMultimap[EntityType.any, Long] = {
    val builder = ImmutableSetMultimap.builder[EntityType.any, Long]()
    modifications collect {
      case modification: EntityModification.Add[_] =>
        builder.put(modification.entityType, modification.entityId)
    }
    builder.build()
  }

  def additionIsPending[E <: Entity: EntityType](entity: E): Boolean = {
    addModificationIds.get(implicitly[EntityType[E]]) contains entity.id
  }

  def ++(otherModifications: Iterable[EntityModification]): PendingModifications =
    copy(modifications = modifications ++ minus(otherModifications, modifications))

  def --(otherModifications: Iterable[EntityModification]): PendingModifications =
    copy(modifications = minus(modifications, otherModifications))

  private def minus[E](a: Iterable[E], b: Iterable[E]): Seq[E] = {
    val bSet = b.toSet
    a.filter(!bSet.contains(_)).toVector
  }
}
