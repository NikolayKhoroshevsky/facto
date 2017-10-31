package flux.react.app.transactionviews

import java.time.Month._

import common.GuavaReplacement.DoubleMath.roundToLong
import common.testing.TestModule
import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import common.time.{DatedMonth, LocalDateTimes, YearRange}
import flux.stores.entries.SummaryExchangeRateGainsStoreFactory.{GainsForMonth, GainsForYear}
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import models.accounting._
import models.accounting.money.ReferenceMoney
import utest._

import scala.collection.immutable.{ListMap, Seq}
import scala2js.Converters._

object SummaryTableTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val summaryTable = testModule.summaryTable
    implicit val fakeClock = testModule.fakeClock
    fakeClock.setTime(createDateTime(2013, JANUARY, 2))

    val allYearsData = summaryTable.AllYearsData(
      allTransactionsYearRange = YearRange.closed(2010, 2013),
      yearsToData = ListMap(
        2012 -> summaryTable.AllYearsData.YearData(
          SummaryForYear(
            Seq(
              createTransaction(year = 2012, month = APRIL, flow = 22, category = testCategoryA),
              createTransaction(year = 2012, month = JUNE, flow = 1.2, category = testCategoryA),
              createTransaction(year = 2012, month = JUNE, flow = -2, category = testCategoryC)
            )),
          GainsForYear(
            monthToGains = Map(DatedMonth.of(2012, JUNE) ->
              GainsForMonth.forSingle(testReservoirCashGbp, ReferenceMoney(123))),
            impactingTransactionIds = Set(),
            impactingBalanceCheckIds = Set()
          )
        ),
        2013 -> summaryTable.AllYearsData.YearData(
          SummaryForYear(
            Seq(
              createTransaction(year = 2013, category = testCategoryA),
              createTransaction(year = 2013, category = testCategoryB))),
          GainsForYear.empty
        )
      )
    )

    "AllYearsData" - {
      "categories" - {
        implicit val account = testAccount.copy(categories = Seq(testCategoryB, testCategoryA))

        allYearsData.categories ==> Seq(testCategoryB, testCategoryA, testCategoryC)
      }
      "cell" - {
        //
      }
      "totalWithoutCategories" - {
        allYearsData
          .totalWithoutCategories(categoriesToIgnore = Set(), month = DatedMonth.of(2012, JUNE)) ==>
          ReferenceMoney(120 - 200 + 123)
        allYearsData
          .totalWithoutCategories(categoriesToIgnore = Set(testCategoryC), month = DatedMonth.of(2012, JUNE)) ==>
          ReferenceMoney(120 + 123)
      }
      "averageWithoutCategories" - {
        "full year" - {
          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 - 200 + 123) / 12))
          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(testCategoryC), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 + 123) / 12))
        }
        "only before June" - {
          fakeClock.setTime(createDateTime(2012, JUNE, 2))

          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(), year = 2012) ==>
            ReferenceMoney(roundToLong(2200.0 / 5))
          allYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(testCategoryC), year = 2012) ==>
            ReferenceMoney(roundToLong(2200.0 / 5))
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange = YearRange.closed(2012, 2013))

          newAllYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 - 200 + 123) / 9))
          newAllYearsData
            .averageWithoutCategories(categoriesToIgnore = Set(testCategoryC), year = 2012) ==>
            ReferenceMoney(roundToLong((2200.0 + 120 + 123) / 9))
        }
      }
      "years" - {
        allYearsData.years ==> Seq(2012, 2013)
      }
      "yearlyAverage" - {
        "full year" - {
          allYearsData.yearlyAverage(2012, testCategoryA) ==>
            ReferenceMoney(roundToLong((2200.0 + 120) / 12))
        }
        "only before June" - {
          fakeClock.setTime(createDateTime(2012, JUNE, 2))
          allYearsData.yearlyAverage(2012, testCategoryA) ==> ReferenceMoney(roundToLong(2200.0 / 5))
        }
        "only after first transaction of year (April)" - {
          val newAllYearsData = allYearsData.copy(allTransactionsYearRange = YearRange.closed(2012, 2013))
          newAllYearsData.yearlyAverage(2012, testCategoryA) ==>
            ReferenceMoney(roundToLong((2200.0 + 120) / 9))
        }
      }
      "monthsForAverage" - {
        //
      }
      "hasExchangeRateGains" - {
        //
      }
      "exchangeRateGains" - {
        //
      }
      "averageExchangeRateGains" - {
        //
      }
      "cell" - {
        //
      }
      "cell" - {
        //
      }
    }
  }

  private def createAllYearsData(yearToTransactions: (Int, Seq[Transaction])*)(
      implicit summaryTable: SummaryTable): summaryTable.AllYearsData = {
    val yearToTransactionsMap = Map(yearToTransactions: _*)
    summaryTable.AllYearsData(
      allTransactionsYearRange =
        YearRange.closed(yearToTransactionsMap.keys.min, yearToTransactionsMap.keys.max),
      yearsToData = ListMap(yearToTransactions.map {
        case (year, transactions) =>
          year -> summaryTable.AllYearsData.YearData(SummaryForYear(transactions), GainsForYear.empty)
      }: _*)
    )
  }

  private final class ThisTestModule extends TestModule {

    import com.softwaremill.macwire._

    private val storesModule = new flux.stores.Module

    implicit val summaryYearsStoreFactory = storesModule.summaryYearsStoreFactory
    implicit val summaryForYearStoreFactory = storesModule.summaryForYearStoreFactory
    implicit val summaryExchangeRateGainsStoreFactory = storesModule.summaryExchangeRateGainsStoreFactory

    val summaryTable: SummaryTable = wire[SummaryTable]
  }
}
