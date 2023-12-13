import io.temporal.client.WorkflowException
import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

object Client:
    def invokeWorkflow(msg: String) = ZIO.serviceWithZIO[ZWorkflowClient]: client =>
        val snowFlake  = SharedUtils.genSnowflake
        val clientName = "client"
        val workflowID = s"$clientName-$snowFlake"
        for
            echoWorkflow <- client.newWorkflowStub[EchoWorkflow](
                ZWorkflowOptions
                    .withWorkflowId(workflowID)
                    .withTaskQueue(TemporalQueues.echoQueue)
                    .withWorkflowRunTimeout(60.seconds)
            )
            _ <- ZIO.logInfo(s"Will submit message \"$msg\" with workflowID $workflowID")

            // Here we execute the workflow and catch any error returning a success to the caller with
            // the processed message or an error message
            res <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg, clientName)).measureTimeConsole("getEcho").catchAll:
                case e: WorkflowException =>
                    ZIO.logError(s"Client: Exceeded retries, error: $e") *> ZIO.succeed("Exceeded retries")
        yield res
        end for
end Client
