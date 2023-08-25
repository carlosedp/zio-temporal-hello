import io.temporal.client.WorkflowException
import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

object WebClient:
  /**
   * This is the main entry point for the web client. It will create a new
   * workflow stub and execute it. The workflow will then call the activity and
   * return the result. The workflowID is generated by the client and passed to
   * the workflow.
   *
   * @param msg
   *   is the message to be sent to the workflow
   * @return
   *   a tuple with the processed message and the workflowID
   */
  def invokeWorkflow(msg: String): ZIO[ZWorkflowClient, Nothing, (String, String)] =
    ZIO.serviceWithZIO[ZWorkflowClient]: client =>
      val snowFlake  = SharedUtils.genSnowflake
      val clientName = "client"
      val workflowID = s"$clientName-$snowFlake"
      for
        echoWorkflow <- client.newWorkflowStub[EchoWorkflow]
          .withTaskQueue(TemporalQueues.echoQueue)
          .withWorkflowId(workflowID)
          .withWorkflowExecutionTimeout(60.seconds)
          .withWorkflowRunTimeout(10.seconds)
          .withRetryOptions(
            ZRetryOptions.default
              .withMaximumAttempts(3)
              .withInitialInterval(300.millis)
              .withBackoffCoefficient(1)
          )
          .build
        _ <- ZIO.logInfo(s"Will submit message \"$msg\" with workflowID $workflowID")

        // Here we execute the workflow and catch any error returning a success to the caller with
        // the processed message or an error message
        res <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg, clientName)).measureTimeConsole("getEcho").catchAll:
          case e: WorkflowException =>
            ZIO.logError(s"Client: Exceeded retries, error: $e") *> ZIO.succeed(
              "Exceeded retries"
            )
      yield (res, snowFlake)
