import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import io.temporal.client.WorkflowException

object Client:
  def invokeWorkflow(msg: String) = ZIO.serviceWithZIO[ZWorkflowClient]: client =>
    for
      snowFlake <- ZIO.succeed(SharedUtils.genSnowflake)
      clientName = "client"
      workflowID = s"$clientName-$snowFlake"
      echoWorkflow <- client
                        .newWorkflowStub[EchoWorkflow]
                        .withTaskQueue(TemporalQueues.echoQueue)
                        .withWorkflowId(workflowID)
                        .withWorkflowRunTimeout(60.seconds)
                        .build
      _ <- ZIO.logInfo(s"Will submit message \"$msg\" with workflowID $workflowID")

      // Here we execute the workflow and catch any error returning a success to the caller with
      // the processed message or an error message
      res <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg, clientName)).measureTimeConsole("getEcho").catchAll:
               case e: WorkflowException =>
                 ZIO.logError(s"Client: Exceeded retries, error: $e") *> ZIO.succeed(
                   "Exceeded retries"
                 )
    yield res
