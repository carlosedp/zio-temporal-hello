import com.softwaremill.id.pretty.{PrettyIdGenerator, StringIdGenerator}
import zio.*
import zio.logging.{LogAnnotation, LogFilter, LogFormat, LoggerNameExtractor}
import zio.temporal.ZLayerAspectSyntax
import zio.temporal.workflow.ZWorkflowServiceStubsOptions

object SharedUtils:
  /**
   * Generate a Snowflake ID which can be sorted
   */
  def genSnowflake =
    lazy val generator: StringIdGenerator = PrettyIdGenerator.singleNode
    generator.nextId()

  /**
   * Set the shared config for ZIO Log filter
   */
  val logFilter: LogFilter[String] = LogFilter.logLevelByName(
    LogLevel.Debug,
    "SLF4J-LOGGER"  -> LogLevel.Info,
    "io.grpc.netty" -> LogLevel.Info,
    "io.netty"      -> LogLevel.Info,
    "io.temporal"   -> LogLevel.Warning,
  )

  val logFormat =
    LogFormat.colored |-| LogFormat.label("source", LoggerNameExtractor.loggerNameAnnotationOrTrace.toLogFormat())
      + LogFormat.logAnnotation(LogAnnotation.UserId)
      + LogFormat.logAnnotation(LogAnnotation.TraceId)

  /**
   * Set the shared config for ZIO Temporal Workflow Service Stubs
   */
  val stubOptions =
    ZWorkflowServiceStubsOptions.make
      @@ ZWorkflowServiceStubsOptions.withServiceUrl(
        scala.util.Properties.envOrElse(
          "TEMPORAL_SERVER",
          "127.0.0.1:7233",
        )
      )
