package controllers

import java.util.concurrent.Executors

import javax.inject._
import play.api.mvc.{Action, AnyContent, InjectedController, Result}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class HomeController extends InjectedController {

  private def doBlocking(): String = {
    Thread.sleep(1000)
    "done"
  }

  private def doBlockingAsync()(implicit ec: ExecutionContext): Future[String] = {
    Future {
      doBlocking()
    }
  }

  private val loggedDefaultExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = {
      println("exec on default pool")
      defaultExecutionContext.execute(runnable)
    }

    def reportFailure(t: Throwable): Unit = {
      defaultExecutionContext.reportFailure(t)
    }
  }

  private val blockingExecutionContext = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(1000)

    def execute(runnable: Runnable): Unit = {
      println("exec on blocking pool")
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable): Unit = { }
  }

  def blockingDefaultThreadPoolNoLogging = Action {
    val result = doBlocking()
    Ok(result)
  }

  def blockingDefaultThreadPool = BlockingAction(loggedDefaultExecutionContext) {
    val result = doBlocking()
    Ok(result)
  }

  def blockingWithBlockingThreadPool = BlockingAction(blockingExecutionContext) {
    val result = doBlocking()
    Ok(result)
  }

  def blockingWrappedDefaultThreadPool = Action.async {
    implicit val ec = loggedDefaultExecutionContext

    doBlockingAsync().map { result =>
      Ok(result)
    }
  }

  private def BlockingAction(executionContext: ExecutionContext)(block: => Result): Action[AnyContent] = {
    Action.async {
      Future(block)(executionContext)
    }
  }

}
