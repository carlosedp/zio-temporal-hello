import zio.*
import zio.temporal.*
import zio.temporal.activity.*

val activityLayer: URLayer[ZActivityOptions[Any], EchoActivity] =
  ZLayer.fromFunction(new EchoActivityImpl()(_: ZActivityOptions[Any]))

@activityInterface
trait EchoActivity:
  @activityMethod
  def echo(msg: String, client: String): Either[Exception, String]

class EchoActivityImpl(
  implicit options: ZActivityOptions[Any],
) extends EchoActivity:
  override def echo(msg: String, client: String = "default"): Either[Exception, String] =
    ZActivity.run:
      for
        _ <- ZIO.logDebug(s"Received from $client, message: $msg") @@ MetricsApp.echoCalls(client)
        // The build of new message might eventually fail
        newMsg <- eventuallyFail(s"ACK: $msg")
      // newMsg <- ZIO.succeed(s"ACK: $msg")
      // _ <- ZIO.logDebug(s"Reply with: $newMsg")
      yield newMsg

  /**
   * Tries to return a message but might randomly fail based on set percentage.
   *
   * @param msg
   *   is the message to be printed
   * @param successPercent
   *   is the percentage of success the effect will have. Defaults to 50%.
   * @return
   *   ZIO[Any, Exception, String]
   */
  def eventuallyFail(msg: String, successPercent: Int = 50): ZIO[Any, Exception, String] =
    require(successPercent >= 0 && successPercent <= 100)
    for
      percent <- Random.nextIntBetween(0, 100)
      r <- percent match
             case p if p < successPercent  => ZIO.succeed(msg)
             case p if p >= successPercent => ZIO.fail(new Exception(msg))
    yield r
