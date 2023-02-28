import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

// This is our workflow interface
@workflowInterface
trait EchoWorkflow:

  @workflowMethod
  def getEcho(str: String, client: String): String

// And here the workflow implementation
class EchoWorkflowImpl extends EchoWorkflow:
  private val echoActivity = ZWorkflow
    .newActivityStub[EchoActivity]
    .withStartToCloseTimeout(5.seconds)
    .build

  override def getEcho(str: String, client: String = "default"): String =
    echoActivity.echo(str, client)
