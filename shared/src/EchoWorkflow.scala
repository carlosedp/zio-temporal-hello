import zio.*
import zio.temporal.*

// This is our workflow interface
@workflowInterface
trait EchoWorkflow {

  @workflowMethod
  def echo(str: String): String
}

// And here the workflow implementation
class EchoWorkflowImpl extends EchoWorkflow:
  override def echo(str: String): String =
    // Log message and increase metric counter
    ZIO.logInfo(s"Echo: $str") @@ MetricsApp.echoCalls(str)
    str
