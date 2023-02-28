import zio.*
import zio.logging.{console, LogFormat, logMetrics}
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> console(LogFormat.colored) ++ logMetrics

  def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
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
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
      )
