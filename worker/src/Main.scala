package worker

import shared.*
import zio.*
import zio.http.*
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.logging.{ConsoleLoggerConfig, consoleLogger, logMetrics}
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.worker.*
import zio.temporal.workflow.*

// ZIO-http server config
val httpPort = 8082
val httpRoutes =
    MetricsApp()
        @@ Middleware.timeout(5.seconds)

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
        // ZLayer.Debug.tree,
    )

object Main extends ZIOAppDefault:
    // Configure ZIO Logging
    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> consoleLogger(
            ConsoleLoggerConfig(SharedUtils.logFormat, SharedUtils.logFilter)
        ) ++ logMetrics

    def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
        val program =
            for
                _ <- ZIO.logInfo(s"HTTP Metrics Server started at http://localhost:$httpPort/metrics")
                _ <- Worker.worker
                _ <- ZWorkflowServiceStubs.setup()
                // Here we setup the worker factory, which will start and progress to the zio-http server which will run forever
                // If the zio-http server is not used, the worker factory should run forever with `ZWorkerFactory.serve` instead.
                _ <- ZWorkerFactory.setup
                // Here the HTTP server is started to serve the worker metrics
                _ <- server
            yield ExitCode.success

        program.provideSome[Scope](
            SharedUtils.stubOptions,
            ZWorkflowClientOptions.make,
            ZWorkerFactoryOptions.make,
            ZWorkflowClient.make,
            ZWorkflowServiceStubs.make,
            ZWorkerFactory.make,
            ZActivityRunOptions.default,
            echoActivityLayer,
            timestampActivityLayer,
            Slf4jBridge.initialize,
            // ZLayer.Debug.tree,
        )
    end run
end Main
