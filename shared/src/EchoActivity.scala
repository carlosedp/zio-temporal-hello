import zio.*
import zio.temporal.*
import zio.temporal.activity.*

val activityLayer: URLayer[ZActivityOptions[Any], EchoActivity] =
  ZLayer.fromFunction(new EchoActivityImpl()(_: ZActivityOptions[Any]))

@activityInterface
trait EchoActivity:
  @activityMethod
  def echo(msg: String, client: String): String

class EchoActivityImpl(implicit options: ZActivityOptions[Any]) extends EchoActivity:
  override def echo(msg: String, client: String = "default"): String =
    val newMsg = s"ACK: $msg"
    ZActivity.run:
      for
        _ <- ZIO.logDebug(s"Received from $client, message: $msg") @@ MetricsApp.echoCalls(client)
        _ <- ZIO.logDebug(s"Reply with: $newMsg")
      yield newMsg
