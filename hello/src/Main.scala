import zio._
import zio.logging.{LogFormat, console, consoleJson, logMetrics}
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._

object Main extends ZIOAppDefault {
  // Configure ZIO Logging
  // override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
  //   Runtime.removeDefaultLoggers >>> console(LogFormat.colored)

  def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program =
      for
        workerFactory <- ZIO.service[ZWorkerFactory]
        workflowResult <- workerFactory.use {
                            workflowResultZIO
                          }
        _ <- ZIO.log(s"The workflow result: $workflowResult")
      yield ExitCode.success

    program
      .provide(
        clientOptions,
        stubOptions,
        workerFactoryOptions,
        helloWorker,
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
      )
  }
}
