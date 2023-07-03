import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*

// This is our workflow interface
@workflowInterface
trait EchoWorkflow:

  @workflowMethod
  def getEcho(msg: String, client: String): String

// And here the workflow implementation
class EchoWorkflowImpl extends EchoWorkflow:
  private val echoActivity = ZWorkflow
    .newActivityStub[EchoActivity]
    .withStartToCloseTimeout(5.seconds)
    .withRetryOptions(
      ZRetryOptions.default
        .withMaximumAttempts(3)
        .withInitialInterval(300.millis)
        .withBackoffCoefficient(1)
    )
    .build

  override def getEcho(msg: String, client: String = "default"): String =
    ZIO.logInfo(s"Worker: Received message in the workflow: \"$msg\"") // TODO: This doesn't print
    ZActivityStub.execute(echoActivity.echo(msg, client))
