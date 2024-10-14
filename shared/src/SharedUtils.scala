package shared

import zio.*
import zio.logging.LogFilter.LogLevelByNameConfig
import zio.logging.{LogAnnotation, LogFilter, LogFormat, LoggerNameExtractor}
import zio.temporal.workflow.ZWorkflowServiceStubsOptions

import com.softwaremill.id.pretty.{PrettyIdGenerator, StringIdGenerator}

object SharedUtils:
  /**
   * Generate a Snowflake ID which can be sorted
   */
  def genSnowflake: String =
    lazy val generator: StringIdGenerator = PrettyIdGenerator.singleNode
    generator.nextId()

  /**
   * Set the shared config for ZIO Log filter
   */
  val logFilter: LogLevelByNameConfig = LogFilter.LogLevelByNameConfig(
    LogLevel.Debug,
    "SLF4J-LOGGER"  -> LogLevel.Info,
    "io.grpc.netty" -> LogLevel.Info,
    "io.netty"      -> LogLevel.Info,
    "io.temporal"   -> LogLevel.Warning,
  )

  val logFormat: LogFormat =
    LogFormat.colored
      + LogFormat.label("source", LoggerNameExtractor.loggerNameAnnotationOrTrace.toLogFormat())
      + LogFormat.logAnnotation(LogAnnotation.UserId)
      + LogFormat.logAnnotation(LogAnnotation.TraceId)

  /**
   * Set the shared config for ZIO Temporal Workflow Service Stubs
   */
  val temporalServer: String = scala.util.Properties.envOrElse(
    "TEMPORAL_SERVER",
    "127.0.0.1:7233",
  )

  val stubOptions: ZLayer[Any, Config.Error, ZWorkflowServiceStubsOptions] =
    ZWorkflowServiceStubsOptions.make
      @@ ZWorkflowServiceStubsOptions.withServiceUrl(
        temporalServer
      )
end SharedUtils
