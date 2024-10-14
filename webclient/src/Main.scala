import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.logging.{ConsoleLoggerConfig, consoleLogger, logMetrics}
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}
import zio.temporal.workflow.*

import shared.{MetricsApp, SharedUtils}

// ZIO-http server config
val httpPort = 8083
val httpRoutes =
  (MetricsApp() ++ FrontEndApp())
    @@ Middleware.timeout(20.seconds)

val httpConfigLayer = ZLayer.succeed(
  Server.Config.default
    .port(httpPort)
)

object Main extends ZIOAppDefault:
  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(SharedUtils.logFormat, SharedUtils.logFilter)
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
      ZLayer.succeed(NettyConfig.default),
      Server.customized,
      publisherLayer,
      prometheusLayer,
      ZWorkflowClient.make,
      ZWorkflowClientOptions.make,
      ZWorkflowServiceStubs.make,
      ZWorkflowServiceStubsOptions.make,
      ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
      // SharedUtils.stubOptions,
      Slf4jBridge.initialize,
    )
  end run
end Main
