package models

import collection.immutable.Seq
import models.manager.EntityManager
import models.accounting._
import models.accounting.money.ExchangeRateMeasurements

object Tables {

  val allEntityManagers: Seq[EntityManager[_, _]] =
    Seq(Users, Transactions, TransactionGroups, BalanceChecks, TagEntities, UpdateLogs, ExchangeRateMeasurements)
}
