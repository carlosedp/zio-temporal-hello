package worker

import java.time
import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import zio.*
import zio.temporal.*
import zio.temporal.activity.*

val timestampActivityLayer: URLayer[ZActivityRunOptions[Any], TimestampActivity] =
    ZLayer.fromFunction(new TimestampActivityImpl()(_: ZActivityRunOptions[Any]))

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
    implicit options: ZActivityRunOptions[Any]
  ) extends TimestampActivity:
    override def timestamp(msg: String): String =
        val now       = ZonedDateTime.now(ZoneOffset.UTC)
        val timestamp = now.format(DateTimeFormatter.ISO_INSTANT)
        ZActivity.run:
            for
                timestampedMsg <- ZIO.succeed(s"[$timestamp] $msg")
                _              <- ZIO.logDebug(s"Worker: Timestamped message: $timestampedMsg")
            yield timestampedMsg
