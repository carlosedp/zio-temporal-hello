import zio.*
import zio.logging.*
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.temporal.*
import zio.temporal.workflow.*

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(LogFormat.colored, SharedUtils.logFilter)
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
        ZLayer.succeed(SharedUtils.stubOptions),
        ZLayer.succeed(ZWorkflowClientOptions.default),
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        Slf4jBridge.initialize,
      )
