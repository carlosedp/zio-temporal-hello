import zio.*
import zio.logging.{consoleLogger, ConsoleLoggerConfig}
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.temporal.*
import zio.temporal.workflow.*

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(
        SharedUtils.logFormat,
        SharedUtils.logFilter,
      )
    )

  def run =
    val program =
      for
        args           <- getArgs
        msg             = if args.isEmpty then "testMsg" else args.mkString(" ")
        workflowResult <- Client.invokeWorkflow(msg)
        _              <- ZIO.log(s"The workflow result: $workflowResult")
      yield ExitCode.success

    program
      .provideSome[ZIOAppArgs](
        SharedUtils.stubOptions,
        ZWorkflowClientOptions.make,
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        Slf4jBridge.initialize,
      )
