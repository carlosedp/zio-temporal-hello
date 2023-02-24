import java.util.UUID

import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._

// This is our workflow interface
@workflowInterface
trait EchoWorkflow {

  @workflowMethod
  def echo(str: String): String
}

// And here the workflow implementation
class EchoWorkflowImpl extends EchoWorkflow {
  override def echo(str: String): String = {
    ZIO.logInfo(s"Echo: $str")
    str
  }
}
