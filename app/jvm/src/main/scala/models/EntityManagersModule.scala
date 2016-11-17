package models

import com.google.inject.AbstractModule
import tools.ApplicationStartHook
import models.accounting._
import models.accounting.money._

final class EntityManagersModule extends AbstractModule {
  override def configure() = {
    bindSingleton(classOf[User.Manager], classOf[SlickUserManager])
    bindSingleton(classOf[BalanceCheck.Manager], classOf[SlickBalanceCheckManager])
    bindSingleton(classOf[TagEntity.Manager], classOf[SlickTagEntityManager])
    bindSingleton(classOf[Transaction.Manager], classOf[SlickTransactionManager])
    bindSingleton(classOf[TransactionGroup.Manager], classOf[SlickTransactionGroupManager])
    bindSingleton(classOf[UpdateLog.Manager], classOf[SlickUpdateLogManager])
    bindSingleton(classOf[ExchangeRateMeasurement.Manager], classOf[SlickExchangeRateMeasurementManager])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
