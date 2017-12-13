package models

import models.accounting._
import models.money.ExchangeRateMeasurement
import models.user.User

abstract class EntityAccess(implicit val userManager: User.Manager,
                            val balanceCheckManager: BalanceCheck.Manager,
                            val transactionManager: Transaction.Manager,
                            val transactionGroupManager: TransactionGroup.Manager,
                            val exchangeRateMeasurementManager: ExchangeRateMeasurement.Manager)