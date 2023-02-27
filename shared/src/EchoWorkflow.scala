import zio.*
import zio.temporal.*
import zio.temporal.workflow.ZWorkflow

// This is our workflow interface
@workflowInterface
trait EchoWorkflow {

  @workflowMethod
  def echo(str: String): String
}

// And here the workflow implementation
class EchoWorkflowImpl extends EchoWorkflow:
  override def echo(str: String): String =
    println(s"Received: $str")
    s"ACK: $str"
