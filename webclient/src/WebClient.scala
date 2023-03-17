import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

object WebClient:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default.withServiceUrl(
      scala.util.Properties.envOrElse("TEMPORAL_SERVER", "127.0.0.1:7233"),
    )

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default

  def workflowStubZIO(client: String): ZIO[ZWorkflowClient, Nothing, ZWorkflowStub.Of[EchoWorkflow]] =
    ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
      workflowClient
        .newWorkflowStub[EchoWorkflow]
        .withTaskQueue(TemporalQueues.echoQueue)
        .withWorkflowId(s"$client-${SharedUtils.genSnowflake}")
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
