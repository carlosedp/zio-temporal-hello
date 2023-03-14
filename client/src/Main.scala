import zio.*
import zio.logging.{LogFormat, console, logMetrics}
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> console(LogFormat.colored, SharedUtils.logFilter) ++ logMetrics

  def run =
    val program =
      for
        args           <- getArgs
        msg             = if args.isEmpty then "testMsg" else args.mkString(" ")
        workerFactory  <- ZIO.service[ZWorkerFactory]
        workflowResult <- workerFactory.use(Client.workflowResultZIO(msg))
        _              <- ZIO.log(s"The workflow result: $workflowResult")
      yield ExitCode.success

    program
      .provideSome[ZIOAppArgs](
        Client.clientOptions,
        Client.stubOptions,
        Client.workerFactoryOptions,
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
      )
