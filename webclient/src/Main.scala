import zio.*
import zio.logging.*
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.http.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

// ZIO-http server config
val httpPort = 8083
val httpRoutes =
  (MetricsApp() ++ FrontEndApp()) @@ HttpAppMiddleware.metrics(MetricsApp.pathLabelMapper) @@ HttpAppMiddleware.timeout(
    5.seconds,
  )

val config: ServerConfig =
  ServerConfig.default
    .port(httpPort)
    .maxThreads(2)

// Define ZIO-http server
val server: ZIO[Any, Throwable, Nothing] = Server
  .serve(httpRoutes)
  .provide( // Add required layers
    ServerConfig.live(config),
    Server.live,
    publisherLayer,
    prometheusLayer,
    WebClient.clientOptions,
    WebClient.stubOptions,
    ZWorkflowClient.make,
    ZWorkflowServiceStubs.make,
    ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
    Slf4jBridge.initialize,
  )

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(LogFormat.colored, SharedUtils.logFilter),
    ) ++ logMetrics

  // Run the application
  def run: ZIO[Scope, Any, ExitCode] =
    for
      _ <- ZIO.logInfo(s"HTTP Metrics Server started at http://localhost:$httpPort/metrics")
      _ <- ZIO.logInfo(s"HTTP Server started at http://localhost:$httpPort")
      _ <- server
    yield ExitCode.success
