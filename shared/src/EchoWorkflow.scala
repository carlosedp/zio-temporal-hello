import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

// This is our workflow interface
@workflowInterface
trait EchoWorkflow:

  @workflowMethod
  def getEcho(str: String, client: String): Either[Exception, String]

// And here the workflow implementation
class EchoWorkflowImpl extends EchoWorkflow:
  private val echoActivity = ZWorkflow
    .newActivityStub[EchoActivity]
    .withStartToCloseTimeout(5.seconds)
    .withRetryOptions(
      ZRetryOptions.default
        .withMaximumAttempts(3)
        .withInitialInterval(300.millis)
        .withBackoffCoefficient(1),
    )
    .build

  override def getEcho(str: String, client: String = "default"): Either[Exception, String] =
    echoActivity.echo(str, client)
