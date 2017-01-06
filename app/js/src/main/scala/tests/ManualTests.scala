package tests

import models.access.LocalDatabaseTest

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport

@JSExport("ManualTests")
object ManualTests {

  @JSExport
  def run(): Unit = {
    var nextFuture = Future.successful((): Unit)
    for (testSuite <- allTestSuites) {
      for (test <- testSuite.tests) {
        nextFuture = nextFuture flatMap { _ =>
          val prefix = s"[${testSuite.getClass.getSimpleName}: ${test.name}]"
          println(s"  $prefix  Starting test")
          Future.successful((): Unit) flatMap { _ =>
            test.testCode()
          } recoverWith {
            case throwable => {
              println(s"  $prefix  Test failed: $throwable")
              throwable.printStackTrace()
              Future.successful((): Unit)
            }
          } map { _ =>
            println(s"  $prefix  Finished test")
          }
        }
      }
    }
  }

  private val allTestSuites: Seq[ManualTestSuite] = Seq(LocalDatabaseTest)

  trait ManualTestSuite {
    def tests: Seq[ManualTest]

    implicit class ArrowAssert[T](lhs: T) {
      def ==>[V](rhs: V) = {
        require(lhs == rhs, s"a ==> b assertion failed: $lhs != $rhs")
      }
    }
  }

  final class ManualTest(val name: String, val testCode: () => Future[Unit])

  object ManualTest {
    def apply(name: String)(testCode: => Future[Unit]): ManualTest = new ManualTest(name, () => testCode)
  }
}
