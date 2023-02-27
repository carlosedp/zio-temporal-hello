import zio.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.temporal.activity.*

val activityLayer: URLayer[ZActivityOptions[Any], EchoActivity] =
  ZLayer.fromFunction(new EchoActivityImpl()(_: ZActivityOptions[Any]))

@activityInterface
trait EchoActivity:
  @activityMethod
  def echo(msg: String): String

class EchoActivityImpl(implicit options: ZActivityOptions[Any]) extends EchoActivity:
  override def echo(msg: String): String =
    ZActivity.run:
      for _ <- ZIO.logDebug(s"Echo: $msg")
      yield msg
