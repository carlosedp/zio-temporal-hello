import com.softwaremill.id.pretty.{PrettyIdGenerator, StringIdGenerator}
import zio.*
import zio.temporal.workflow.ZWorkflowServiceStubsOptions
import zio.logging.LogFilter

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
    "SLF4J-LOGGER"                                -> LogLevel.Warning,
    "io.grpc.netty"                               -> LogLevel.Warning,
    "io.grpc.netty.shaded.io.netty.util.internal" -> LogLevel.Warning,
    "io.netty"                                    -> LogLevel.Warning,
    "io.temporal"                                 -> LogLevel.Error,
    "io.temporal.internal.worker.Poller"          -> LogLevel.Error,
    "zio.temporal.internal"                       -> LogLevel.Info,
  )

  val stubOptions = ZWorkflowServiceStubsOptions.default.withServiceUrl(
    scala.util.Properties.envOrElse("TEMPORAL_SERVER", "127.0.0.1:7233")
  )
