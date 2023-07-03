import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.NettyConfig.LeakDetectionLevel
import zio.logging.*
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}
import zio.temporal.*
import zio.temporal.workflow.*

// ZIO-http server config
val httpPort = 8083
val httpRoutes =
  (MetricsApp() ++ FrontEndApp())
    @@ HttpAppMiddleware.metrics(MetricsApp.pathLabelMapper)
    @@ HttpAppMiddleware.timeout(20.seconds)

val httpConfigLayer = ZLayer.succeed(
  Server.Config.default
    .port(httpPort)
)

val nettyConfigLayer = ZLayer.succeed(
  NettyConfig.default
    .leakDetection(LeakDetectionLevel.SIMPLE)
    .maxThreads(4)
)

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(LogFormat.colored, SharedUtils.logFilter)
    ) ++ logMetrics

  // Run the application
  def run: ZIO[Scope, Any, ExitCode] =
    val program =
      for
        _ <- ZIO.logInfo(s"HTTP Metrics Server started at http://localhost:$httpPort/metrics")
        _ <- ZIO.logInfo(s"HTTP Server started at http://localhost:$httpPort")
        _ <- Server.serve(httpRoutes)
      yield ExitCode.success

    program.provide(
      httpConfigLayer,
      nettyConfigLayer,
      Server.customized,
      publisherLayer,
      prometheusLayer,
      ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
      SharedUtils.stubOptions,
      ZWorkflowClientOptions.make,
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make,
      Slf4jBridge.initialize,
    )
