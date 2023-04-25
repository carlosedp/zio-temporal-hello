import zio.*
import zio.logging.*
import zio.http.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.temporal.activity.*
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

// ZIO-http server config
val httpPort = 8082
val httpRoutes =
  MetricsApp()
    @@ HttpAppMiddleware.metrics(MetricsApp.pathLabelMapper)
    @@ HttpAppMiddleware.timeout(5.seconds)

val httpConfigLayer = ZLayer.succeed(
  Server.Config.default
    .port(httpPort)
)

// Define ZIO-http server
val server: ZIO[Any, Throwable, Nothing] = Server
  .serve(httpRoutes)
  .provide( // Add required layers for the http server
    httpConfigLayer,
    Server.live,
    publisherLayer,
    prometheusLayer,
    ZLayer.succeed(MetricsConfig(500.millis)), // Metrics pull interval from internal store
  )

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(LogFormat.colored, SharedUtils.logFilter)
    ) ++ logMetrics

  def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    val program =
      for
        _ <- ZIO.logInfo(s"HTTP Metrics Server started at http://localhost:$httpPort/metrics")
        _ <- WorkerModule.worker
        _ <- ZWorkflowServiceStubs.setup()
        _ <- ZWorkerFactory.setup
        _ <- server
      yield ExitCode.success

    program
      .provideSome[Scope](
        ZLayer.succeed(SharedUtils.stubOptions),
        ZLayer.succeed(ZWorkflowClientOptions.default),
        ZLayer.succeed(ZWorkerFactoryOptions.default),
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        ZActivityOptions.default,
        activityLayer,
        // slf4j.bridge.Slf4jBridge.initialize,
      )
