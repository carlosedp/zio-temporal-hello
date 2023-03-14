import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.worker.*

object Client:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default.withServiceUrl(
      scala.util.Properties.envOrElse("TEMPORAL_SERVER", "127.0.0.1:7233"),
    )

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default

  val workerFactoryOptions: ULayer[ZWorkerFactoryOptions] = ZLayer.succeed:
    ZWorkerFactoryOptions.default

  def workflowStubZIO(client: String, workflowID: String) = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
    workflowClient
      .newWorkflowStub[EchoWorkflow]
      .withTaskQueue(TemporalQueues.echoQueue)
      .withWorkflowId(s"$client-$workflowID")
      .withWorkflowRunTimeout(2.seconds)
      .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3).withBackoffCoefficient(1))
      .build

  def workflowResultZIO(msg: String) =
    val workflowID = SharedUtils.genSnowflake
    val client     = "client"
    for
      echoWorkflow <- workflowStubZIO(client, workflowID)
      _            <- ZIO.logInfo(s"Will submit message \"$msg\" with workflowID $client-$workflowID")
      result       <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg, client)).measureTimeConsole("getEcho")
    yield result
