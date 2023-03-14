import zio.*
import zio.logging.{console, LogFormat, LogFilter, logMetrics}
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.http.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.temporal.activity.*
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

// ZIO-http server config
val httpPort   = 8082
val httpRoutes = (MetricsApp()) @@ Middleware.metrics(MetricsApp.pathLabelMapper) @@ Middleware.timeout(5.seconds)
val config: ServerConfig =
  ServerConfig.default
    .port(httpPort)
    .leakDetection(ServerConfig.LeakDetectionLevel.PARANOID)
    .maxThreads(2)

// Define ZIO-http server
val server: ZIO[Any, Throwable, Nothing] = Server
  .serve(httpRoutes)
  .provide( // Add required layers
    ServerConfig.live(config),
    Server.live,
    publisherLayer,
    prometheusLayer,
    ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
  )

object Main extends ZIOAppDefault:
  // Configure ZIO Logging

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> console(
      LogFormat.colored,
      SharedUtils.logFilter,
    ) ++ logMetrics

  def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    val program =
      for
        _             <- ZIO.logInfo(s"HTTP Metrics Server started at http://localhost:$httpPort/metrics")
        workerFactory <- ZIO.service[ZWorkerFactory]
        _             <- workerFactory.use(server)
      yield ExitCode.success

    program
      .provide(
        WorkerModule.clientOptions,
        WorkerModule.stubOptions,
        WorkerModule.workerFactoryOptions,
        WorkerModule.worker,
        ZWorkflowServiceStubs.make,
        ZWorkflowClient.make,
        ZWorkerFactory.make,
        ZActivityOptions.default,
        activityLayer,
        Slf4jBridge.initialize,
      )
