import io.temporal.client.WorkflowException
import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

object WebClient:
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
