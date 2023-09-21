import java.time
import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import zio.*
import zio.temporal.*
import zio.temporal.activity.*

val timestampActivityLayer: URLayer[ZActivityOptions[Any], TimestampActivity] =
  ZLayer.fromFunction(new TimestampActivityImpl()(_: ZActivityOptions[Any]))

@activityInterface
trait TimestampActivity:
  /**
   * Adds a timestamp to the message body.
   *
   * @param msg
   * @return
   *   the message echoed back with a timestamp
   */
  def timestamp(msg: String): String

class TimestampActivityImpl(
    implicit options: ZActivityOptions[Any]
  ) extends TimestampActivity:
  override def timestamp(msg: String): String =
    val now       = ZonedDateTime.now(ZoneOffset.UTC)
    val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)
    ZActivity.run:
      for
        timestampedMsg <- ZIO.succeed(s"[$timestamp] $msg")
        _              <- ZIO.logDebug(s"Worker: Timestamped message: $timestampedMsg")
      yield timestampedMsg
