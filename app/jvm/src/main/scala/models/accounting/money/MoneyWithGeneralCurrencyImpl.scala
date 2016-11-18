package models.accounting.money

import play.twirl.api.Html

/**
  * Represents an amount of money with an arbitrary currency.
  *
  * Note that this can't be converted into other currencies since we don't know what date we should assume for the
  * exchange rate.
  */
private[money] case class MoneyWithGeneralCurrencyImpl(override val cents: Long, currency: Currency) extends MoneyWithGeneralCurrency {

  override def toHtmlWithCurrency(implicit exchangeRateManager: ExchangeRateManager): Html = {
    Money.centsToHtmlWithCurrency(cents, currency)
  }
}
