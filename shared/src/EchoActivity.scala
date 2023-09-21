import zio.*
import zio.temporal.*
import zio.temporal.activity.*

val echoActivityLayer: URLayer[ZActivityOptions[Any], EchoActivity] =
  ZLayer.fromFunction(new EchoActivityImpl()(_: ZActivityOptions[Any]))

@activityInterface
trait EchoActivity:
  /**
   * Echoes a message back to the caller. The message could randomly fail.
   *
   * @param msg
   * @param client
   * @return
   *   the message echoed back with an ACK prefix
   */
  def echo(msg: String, client: String): String

class EchoActivityImpl(
    implicit options: ZActivityOptions[Any]
  ) extends EchoActivity:
  override def echo(msg: String, client: String = "default"): String =
    ZActivity.run:
      for
        _      <- ZIO.logDebug(s"Received from $client, message: $msg") @@ MetricsApp.echoActivityCall(client)
        newMsg <- eventuallyFail(s"ACK: $msg", 40) // The build of new message might eventually fail
        _      <- ZIO.logDebug(s"Worker: Reply with: $newMsg")
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
      _       <- ZIO.logDebug(s"Worker: Generated percent is $percent")
      _ <- ZIO.when(percent > successPercent):
        ZIO.logError("Worker: eventuallyFail - Failed to process message") *> ZIO.fail(
          Exception(s"Worker: ERROR: $msg")
        )
      _ <- ZIO.logInfo("Worker: Success processing message")
    yield msg
