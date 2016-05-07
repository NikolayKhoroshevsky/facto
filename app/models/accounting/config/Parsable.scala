package models.accounting.config

import java.util.Collections

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

import com.google.common.collect.ImmutableList
import play.twirl.api.Html

import models.accounting.config.{Config => ParsedConfig, Account => ParsedAccount, Category => ParsedCategory, MoneyReservoir => ParsedMoneyReservoir,
Template => ParsedTemplate, Constants => ParsedConstants}
import models.accounting.config.Account.{SummaryTotalRowDef => ParsedSummaryTotalRowDef}
import models.accounting.config.MoneyReservoir.NullMoneyReservoir
import models.accounting.Money

object Parsable {

  case class Config(accounts: java.util.List[Account],
                    categories: java.util.List[Category],
                    moneyReservoirs: java.util.List[MoneyReservoir],
                    templates: java.util.List[Template],
                    constants: Constants) {
    def this() = this(null, null, null, null, null)

    def parse: ParsedConfig = {
      val parsedAccounts = toListMap(accounts)(_.code, _.parse)
      val parsedCategories = toListMap(categories)(_.code, _.parse)
      val parsedReservoirs = toListMap(moneyReservoirs)(_.code, _.parse)
      val parsedTemplates = templates.asScala.toVector map (_.parse(parsedAccounts, parsedReservoirs, parsedCategories))
      ParsedConfig(parsedAccounts, parsedCategories, parsedReservoirs, parsedTemplates, constants.parse)
    }
  }

  case class Account(code: String,
                     longName: String,
                     shorterName: String,
                     veryShortName: String,
                     userLoginName: /* nullable */ String,
                     categories: java.util.List[Category],
                     summaryTotalRows: /* nullable */ java.util.List[Account.SummaryTotalRowDef]) {
    def this() = this(null, null, null, null, null, null, null)

    def parse: ParsedAccount = {
      var nonNullSummaryTotalRows = if (summaryTotalRows == null) ImmutableList.of(Account.SummaryTotalRowDef.default) else summaryTotalRows
      ParsedAccount(
        code = code,
        longName = longName,
        shorterName = shorterName,
        veryShortName = veryShortName,
        userLoginName = if (userLoginName == null) None else Some(userLoginName),
        categories = categories.asScala.toList.map(_.parse),
        summaryTotalRows = nonNullSummaryTotalRows.asScala.toList.map(_.parse))
    }
  }
  object Account {
    case class SummaryTotalRowDef(rowTitleHtml: String, categoriesToIgnore: java.util.List[Category]) {
      def this() = this(null, null)

      def parse: ParsedSummaryTotalRowDef = ParsedSummaryTotalRowDef(
        rowTitleHtml = Html(rowTitleHtml),
        categoriesToIgnore = categoriesToIgnore.asScala.map(_.parse).toSet)
    }
    object SummaryTotalRowDef {
      val default: SummaryTotalRowDef = SummaryTotalRowDef("<b>Total</b>", Collections.emptyList[Category])
    }
  }

  case class Category(code: String, name: String, helpText: String) {
    def this() = this(null, null, helpText = "")

    def parse: ParsedCategory = {
      ParsedCategory(code, name, helpText)
    }
  }


  case class MoneyReservoir(code: String, name: String, shorterName: /* nullable */ String, owner: Account, hidden: Boolean) {
    def this() = this(null, null, null, null, hidden = false)

    def parse: ParsedMoneyReservoir = {
      val parsedShorterName = if (shorterName == null) name else shorterName
      ParsedMoneyReservoir(code, name, parsedShorterName, owner.parse, hidden)
    }
  }

  case class Template(name: String,
                      placement: java.util.List[String],
                      onlyShowForUserLoginNames: /* nullable */ java.util.List[String],
                      zeroSum: Boolean,
                      transactions: java.util.List[Template.Transaction]) {

    def this() = this(null, null, null, zeroSum = false, null)

    def parse(accounts: Map[String, ParsedAccount],
              reservoirs: Map[String, ParsedMoneyReservoir],
              categories: Map[String, ParsedCategory]): ParsedTemplate = {
      ParsedTemplate(
        nameTpl = name,
        placement = placement.asScala.toSet map ParsedTemplate.Placement.fromString,
        onlyShowForUserLoginNames = Option(onlyShowForUserLoginNames) map (_.asScala.toSet),
        zeroSum = zeroSum,
        transactions = transactions.asScala.toList map (_.parse(accounts, reservoirs, categories)))
    }
  }

  object Template {
    case class Transaction(beneficiaryCode: /* nullable */ String,
                           moneyReservoirCode: /* nullable */ String,
                           categoryCode: /* nullable */ String,
                           description: String,
                           flowInCents: Long) {
      def this() = this(null, null, null, description = "", flowInCents = 0)

      def parse(accounts: Map[String, ParsedAccount],
                reservoirs: Map[String, ParsedMoneyReservoir],
                categories: Map[String, ParsedCategory]): ParsedTemplate.Transaction = {
        def validateCode(values: Set[String])(code: String): String = {
          if (!(code contains "$")) {
            require(values contains code, s"Illegal code '$code' (possibilities = $values)")
          }
          code
        }
        val reservoirsIncludingNull = reservoirs ++ Map(NullMoneyReservoir.code -> NullMoneyReservoir)
        ParsedTemplate.Transaction(
          beneficiaryCodeTpl = Option(beneficiaryCode) map validateCode(accounts.keySet),
          moneyReservoirCodeTpl = Option(moneyReservoirCode) map validateCode(reservoirsIncludingNull.keySet),
          categoryCodeTpl = Option(categoryCode) map validateCode(categories.keySet),
          descriptionTpl = description,
          flowInCents = flowInCents)
      }
    }
  }


  case class Constants(commonAccount: Account,
                       accountingCategory: Category,
                       endowmentCategory: Category,
                       defaultElectronicMoneyReservoirs: java.util.List[MoneyReservoir],
                       defaultCurrencySymbol: String,
                       liquidationDescription: String) {
    def this() = this(null, null, null, null, null, liquidationDescription = "Liquidation")

    def parse: ParsedConstants = {
      ParsedConstants(
        commonAccount = commonAccount.parse,
        accountingCategory = accountingCategory.parse,
        endowmentCategory = endowmentCategory.parse,
        defaultElectronicMoneyReservoirByAccount = toListMap(defaultElectronicMoneyReservoirs)(_.parse.owner, _.parse),
        liquidationDescription = liquidationDescription,
        defaultCurrencySymbol = defaultCurrencySymbol)
    }
  }


  private def toListMap[T, K, V](list: java.util.List[T])(keyGetter: T => K, valueGetter: T => V): ListMap[K, V] = {
    val tuples = list.asScala.map(t => (keyGetter(t), valueGetter(t)))
    val resultBuilder = ListMap.newBuilder[K, V]
    tuples.foreach(resultBuilder += _)
    resultBuilder.result
  }
}