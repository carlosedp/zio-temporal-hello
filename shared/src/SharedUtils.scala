import com.softwaremill.id.pretty.{PrettyIdGenerator, StringIdGenerator}
import zio.*
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
    "io.temporal"                                 -> LogLevel.Warning,
    "io.temporal.internal.worker.Poller"          -> LogLevel.Error,
  )
