package models.accounting.money

import com.google.inject._
import common.Clock
import Currency.{Eur, Gbp, Usd}
import common.cache.CacheRegistry
import common.testing._
import common.testing.TestUtils.persistGbpMeasurement
import org.joda.time.DateTime
import org.specs2.mutable._
import play.api.test.WithApplication
import play.twirl.api.Html
import models._

class ExchangeRateManagerTest extends CacheClearingSpecification {

  @Inject implicit val entityAccess: EntityAccess = null

  @Inject val exchangeRateManager: ExchangeRateManager = null

  override def beforeEveryTest() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "getRatioSecondToFirstCurrency()" in new WithApplication {
    persistGbpMeasurement(millisSinceEpoch = 1000, ratio = 2)
    persistGbpMeasurement(millisSinceEpoch = 2000, ratio = 3)
    persistGbpMeasurement(millisSinceEpoch = 3000, ratio = 0.5)

    exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Eur, DateTime.now) mustEqual 1.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Usd, DateTime.now) mustEqual 1.0

    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(0)) mustEqual 1.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(999)) mustEqual 1.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(1000)) mustEqual 2.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(1001)) mustEqual 2.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, new DateTime(2000)) mustEqual 3.0
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now) mustEqual 0.5

    exchangeRateManager.getRatioSecondToFirstCurrency(Eur, Gbp, DateTime.now) mustEqual 2.0

    persistGbpMeasurement(millisSinceEpoch = 4000, ratio = 9)
    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now) mustEqual 9.0
  }

  "Verify consistency" in new WithApplication {
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    persistGbpMeasurement(millisSinceEpoch = 1000, ratio = 2)
    persistGbpMeasurement(millisSinceEpoch = 2000, ratio = 3)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    persistGbpMeasurement(millisSinceEpoch = 3000, ratio = 4)
    CacheRegistry.doMaintenanceAndVerifyConsistency()

    exchangeRateManager.getRatioSecondToFirstCurrency(Gbp, Eur, DateTime.now)
    CacheRegistry.doMaintenanceAndVerifyConsistency()
  }
}
