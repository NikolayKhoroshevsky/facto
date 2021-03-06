package api

import api.Picklers._
import api.ScalaJsApi._
import api.UpdateTokens.toUpdateToken
import com.google.inject._
import common.PlayI18n
import common.money.Currency
import common.time.{Clock, LocalDateTime}
import models.Entity
import models.access.{DbQuery, JvmEntityAccess}
import models.accounting.config.Config
import models.modification.{EntityModification, EntityType}
import models.money.ExchangeRateMeasurement
import models.user.{User, Users}

import scala.collection.immutable.{Seq, TreeMap}
import scala.collection.mutable

final class ScalaJsApiServerFactory @Inject()(implicit accountingConfig: Config,
                                              clock: Clock,
                                              entityAccess: JvmEntityAccess,
                                              i18n: PlayI18n) {

  def create()(implicit user: User): ScalaJsApi = new ScalaJsApi() {

    override def getInitialData() =
      GetInitialDataResponse(
        accountingConfig = accountingConfig,
        user = user,
        allUsers = entityAccess.newQuerySync[User]().data(),
        i18nMessages = i18n.allI18nMessages,
        ratioReferenceToForeignCurrency = {
          val mapBuilder =
            mutable.Map[Currency, mutable.Builder[(LocalDateTime, Double), TreeMap[LocalDateTime, Double]]]()
          for (measurement <- entityAccess.newQuerySync[ExchangeRateMeasurement]().data()) {
            val currency = measurement.foreignCurrency
            if (!(mapBuilder contains currency)) {
              mapBuilder(currency) = TreeMap.newBuilder[LocalDateTime, Double]
            }
            mapBuilder(currency) += (measurement.date -> measurement.ratioReferenceToForeignCurrency)
          }
          mapBuilder.toStream.map { case (k, v) => k -> v.result() }.toMap
        },
        nextUpdateToken = toUpdateToken(clock.now)
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = toUpdateToken(clock.now)
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType => {
            entityType -> entityAccess.newQuerySync()(entityType).data()
          })
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = {
      // check permissions
      for (modification <- modifications) {
        require(
          !modification.isInstanceOf[EntityModification.Update[_]],
          "Update modifications are not allowed to be created by clients " +
            "(see EntityModification.Update documentation)"
        )
        require(
          modification.entityType != EntityType.UserType,
          "Please modify users by calling upsertUser() instead")
        require(
          modification.entityType != EntityType.ExchangeRateMeasurementType,
          "Client initiated exchange rate measurement changes are not allowed")
      }

      entityAccess.persistEntityModifications(modifications)
    }

    override def executeDataQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].data(query)
      }
      internal
    }

    override def executeCountQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].count(query)
      }
      internal
    }

    override def upsertUser(userProto: UserPrototype): Unit = {
      def requireNonEmpty(s: Option[String]): Unit = {
        require(s.isDefined, "field is missing")
        require(s.get.nonEmpty, "field contains an empty string")
      }
      def requireNonEmptyIfSet(s: Option[String]): Unit = {
        if (s.isDefined) {
          require(s.get.nonEmpty, "field contains an empty string")
        }
      }

      userProto.id match {
        case None => // Add user
          requireNonEmpty(userProto.loginName)
          requireNonEmpty(userProto.plainTextPassword)
          requireNonEmpty(userProto.name)

          // Check permissions
          require(user.isAdmin, "Only an admin can add users")

          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(Users.createUser(
              loginName = userProto.loginName.get,
              password = userProto.plainTextPassword.get,
              name = userProto.name.get,
              isAdmin = userProto.isAdmin getOrElse false,
              expandCashFlowTablesByDefault = userProto.expandCashFlowTablesByDefault getOrElse true,
              expandLiquidationTablesByDefault = userProto.expandLiquidationTablesByDefault getOrElse true
            )))

        case Some(id) => // Update user
          requireNonEmptyIfSet(userProto.loginName)
          requireNonEmptyIfSet(userProto.plainTextPassword)
          requireNonEmptyIfSet(userProto.name)

          // Check permissions
          require(user.isAdmin || id == user.id, "Changing an other user's password")

          val existingUser = entityAccess.newQuerySync[User]().findById(id)
          val updatedUser = {
            var result = existingUser.copy(
              loginName = userProto.loginName getOrElse existingUser.loginName,
              name = userProto.name getOrElse existingUser.name,
              isAdmin = userProto.isAdmin getOrElse existingUser.isAdmin,
              expandCashFlowTablesByDefault =
                userProto.expandCashFlowTablesByDefault getOrElse existingUser.expandCashFlowTablesByDefault,
              expandLiquidationTablesByDefault =
                userProto.expandLiquidationTablesByDefault getOrElse
                  existingUser.expandLiquidationTablesByDefault
            )
            if (userProto.plainTextPassword.isDefined) {
              result = Users.copyUserWithPassword(result, userProto.plainTextPassword.get)
            }
            result
          }

          entityAccess.persistEntityModifications(EntityModification.createUpdate(updatedUser))
      }
    }
  }
}
