import zio.*
import zio.logging.{console, LogFormat, logMetrics}
import zio.http.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

// ZIO-http server config
val httpRoutes =
  (MetricsApp() ++ FrontEndApp()) @@ Middleware.metrics(MetricsApp.pathLabelMapper) @@ Middleware.timeout(5.seconds)
val httpPort = 8083
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
    clientOptions,
    stubOptions,
    // workerFactoryOptions,
    ZWorkflowClient.make,
    ZWorkflowServiceStubs.make,
    // ZWorkerFactory.make,
    ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
  )

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> console(LogFormat.colored) ++ logMetrics

  def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    val program =
      for
        _             <- ZIO.logInfo(s"Server started in http://localhost:$httpPort")
        workerFactory <- ZIO.service[ZWorkerFactory]
        _             <- workerFactory.use(server)
      yield ()

    program
      .provide(
        clientOptions,
        stubOptions,
        workerFactoryOptions,
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
      )
