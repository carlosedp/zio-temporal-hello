import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

object WebClient:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default

  def workflowStubZIO(client: String) = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
    workflowClient
      .newWorkflowStub[EchoWorkflow]
      .withTaskQueue(TemporalQueues.echoQueue)
      .withWorkflowId(s"$client-${genSnowflake}")
      .withWorkflowRunTimeout(2.seconds)
      .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3).withBackoffCoefficient(1))
      .build

  def callEchoWorkflow(msg: String, client: String = "default"): ZIO[ZWorkflowClient, Nothing, String] =
    for
      _            <- ZIO.logDebug(s"Will submit message \"$msg\"")
      echoWorkflow <- workflowStubZIO(client)
      result <- ZWorkflowStub
                  .execute(echoWorkflow.getEcho(msg, client))
                  .measureTimeConsole("getEcho")
                  .orElseSucceed("Error calling workflow")
    yield result
