package spatutorial.client

import scala.collection.mutable
import java.time.{DayOfWeek, LocalDate, LocalTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalField}

import api.ScalaJsApi.EntityType
import api.ScalaJsApi.EntityType._
import api.ScalaJsApiClient
import models.User
import models.accounting.{Tag, Transaction}
import org.scalajs.dom

//import spatutorial.client.components.GlobalStyles
import spatutorial.client.logger._

//import spatutorial.client.modules._
//import spatutorial.client.services.SPACircuit

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.annotation.meta.field
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scala.concurrent.ExecutionContext.Implicits.global

import jsfacades._
import scala2js.Scala2Js
import scala2js.Converters._
import scala.collection.immutable.Seq

import common._
import common.time._
import common.time.JavaTimeImplicits._
import java.time.Month._
import common.time.LocalDateTime
import java.time.format._

@JSExport("SPAMain")
object SPAMain extends js.JSApp {

  // Define the locations (pages) used in this application
  sealed trait Loc

  case object DashboardLoc extends Loc

  case object TodoLoc extends Loc

  //  // configure the router
  //  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
  //    import dsl._
  //
  //    val todoWrapper = SPACircuit.connect(_.todos)
  //    // wrap/connect components to the circuit
  //    (staticRoute(root, DashboardLoc) ~> renderR(ctl => SPACircuit.wrap(_.motd)(proxy => Dashboard(ctl, proxy)))
  //      | staticRoute("#todo", TodoLoc) ~> renderR(ctl => todoWrapper(Todo(_)))
  //      ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
  //  }.renderWith(layout)
  //
  //  val todoCountWrapper = SPACircuit.connect(_.todos.map(_.items.count(!_.completed)).toOption)
  //  // base layout for all pages
  //  def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
  //    <.div(
  //      // here we use plain Bootstrap class names as these are specific to the top level layout defined here
  //      <.nav(^.className := "navbar navbar-inverse navbar-fixed-top",
  //        <.div(^.className := "container",
  //          <.div(^.className := "navbar-header", <.span(^.className := "navbar-brand", "SPA Tutorial")),
  //          <.div(^.className := "collapse navbar-collapse",
  //            // connect menu to model, because it needs to update when the number of open todos changes
  //            todoCountWrapper(proxy => MainMenu(c, r.page, proxy))
  //          )
  //        )
  //      ),
  //      // currently active module is shown in this container
  //      <.div(^.className := "container", r.render())
  //    )
  //  }

  @JSExport
  def main(): Unit = {
    //     log.warn("Application starting")
    //    // send log messages also to the server
    //    log.enableServerLogging("/logging")
    //    log.info("This message goes to server as well")
    //
    //    // create stylesheet
    //    GlobalStyles.addToDocument()
    //    // create the router
    //    val router = Router(BaseUrl.until_#, routerConfig)
    //    // tell React to render the router in the document body
    //    ReactDOM.render(router(), dom.document.getElementById("root"))
  }

  @JSExport
  def factoPortabilityTest(): Unit = {
    out("factoPortabilityTest() starting")

    // --------------------------- Check for linker errors --------------------------- //
    implicit val clock = new JsClock
    val date = LocalDateTime.of(2012, DECEMBER, 1, 0, 0)

    out(clock.now)
    out(date)
    TimeUtils.requireStartOfMonth(LocalDate.of(2010, NOVEMBER, 1))
    out(TimeUtils.allMonths)
    out(TimeUtils.parseDateString("2012-08-01"))
    out(DatedMonth.containing(LocalDateTime.of(2100, DECEMBER, 1, 0, 0)).contains(LocalDateTime.of(2100, DECEMBER, 10, 0, 0)))
    out(DatedMonth.allMonthsIn(2012))
    out(MonthRange.forYear(2012) intersection MonthRange.atLeast(DatedMonth.containing(date)))
    out(Tag.parseTagsString("ab,c"))

    implicit val i18n = new I18n {
      override def apply(key: String, args: Any*): String = key
    }
    out(Formatting.formatDateTime(date))
    out(Formatting.formatDate(date))
    out(Formatting.formatDateTime(clock.now))
    out(Formatting.formatDate(clock.now))

    assertException(classOf[IllegalArgumentException], X(null))
    X("abc")

    // --------------------------- Test MacWire --------------------------- //
    UserModule.otherUserStatusReader.out()

    // --------------------------- Test Scala2Js --------------------------- //
    val transaction: Transaction = Scala2Js.toScala[Transaction](js.JSON.parse(
      """{
          "transactionGroupId": 1232138,
          "issuerId": 32978,
          "beneficiaryAccountCode": "abc",
          "moneyReservoirCode": "def",
          "categoryCode": "ghi",
          "description": "safd",
          "flowInCents": "8837432",
          "detailDescription": "jkfdsa",
          "tagsString": "",
          "createdDate": 1237897,
          "transactionDate": 1481666404,
          "consumedDate": 1481666404,
          "id": 43334
        }
      """))
    out(transaction)
    val transactionJs = Scala2Js.toJs(transaction)
    outJs(transactionJs)
    out(Scala2Js.toScala[Transaction](transactionJs))

    // --------------------------- Test LocalDatabase --------------------------- //
    out(EntityType.BalanceCheckType.name)
    out(EntityType.BalanceCheckType)
    out(EntityType.BalanceCheckType.entityClass)
    assertException(classOf[IllegalArgumentException], EntityType.BalanceCheckType.checkRightType(transaction))
    EntityType.TransactionType.checkRightType(transaction)

    // --------------------------- Test ScalaJsApi --------------------------- //
    new ScalaJsApiClient().getAccountingConfig().foreach(out)
    new ScalaJsApiClient().getAllEntities(Seq(UserType)).foreach(users => out(s"Users: $users"))
    new ScalaJsApiClient().insertEntityWithId(UserType)(User("blah", "pw", "name", Option(2283)))

    // --------------------------- Test Loki --------------------------- //
    val db = Loki.Database.persistent("loki-in-scalajs-test")
    def save(callback: () => Unit = () => {}) = {
      val transactionsCollection = db.getOrAddCollection("transactions")
      new ScalaJsApiClient().getAllEntities(Seq(TransactionType)).foreach(resultMap => {
        val transactions = resultMap(TransactionType).asInstanceOf[Seq[Transaction]]
        for (transaction <- transactions) {
          transactionsCollection.insert(Scala2Js.toJs(transaction))
        }
        db.saveDatabase(() => {
          out("Done saving transactions")
          callback()
        })
      })
    }
    def load() = {
      out("loading")
      db.loadDatabase()
      db.loadDatabase(() => {
        out("loaded")
        val children = db.getOrAddCollection("transactions")
        //        out(children.find(js.Dictionary("categoryCode" -> "MED")).toSeq map (Scala2Js.toScala[Transaction]))
      })
    }
    //    save(() => load())
    //        load()
  }

  def outJs(x: js.Any): Unit = {
    out(js.JSON.stringify(x))
  }

  def out(x: Any): Unit = {
    println(s"  $x")
  }

  def assertException(exceptionType: Class[_ <: Exception], expression: => Unit): Unit = {
    try {
      expression
      throw new AssertionError()
    } catch {
      case e: Exception => require(e.getClass == exceptionType)
    }
  }

  case class X(s: String) {
    Require.requireNonNull(s)
  }

  //////////////// MacWire test ///////////////////
  trait DatabaseAccess {
    def out(): Unit
  }
  class DatabaseAccessImpl() extends DatabaseAccess {
    override def out() = SPAMain.out("IMPL!")
  }
  class SecurityFilter() {
  }
  class UserFinder(val databaseAccess: DatabaseAccess, securityFilter: SecurityFilter) {
  }
  class UserStatusReader(userFinder: UserFinder, databaseAccess: DatabaseAccess, securityFilter: SecurityFilter) {
    def out() = userFinder.databaseAccess.out()
  }

  object UserModule {

    import com.softwaremill.macwire._

    lazy val theDatabaseAccess = wire[DatabaseAccessImpl]
    lazy val theSecurityFilter = wire[SecurityFilter]
    lazy val theUserFinder = wire[UserFinder]
    lazy val theUserStatusReader = wire[UserStatusReader]
    lazy val otherUserStatusReader = wire[UserStatusReader]
  }
}
