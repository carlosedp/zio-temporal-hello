import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.worker.*

object Client:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default

  val workerFactoryOptions: ULayer[ZWorkerFactoryOptions] = ZLayer.succeed:
    ZWorkerFactoryOptions.default

  val workflowID = genSnowflake
  def workflowStubZIO(client: String) = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
    workflowClient
      .newWorkflowStub[EchoWorkflow]
      .withTaskQueue(TemporalQueues.echoQueue)
      .withWorkflowId(s"$client-${workflowID}")
      .withWorkflowRunTimeout(2.seconds)
      .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3).withBackoffCoefficient(1))
      .build

  val msg = "Hello there"
  val workflowResultZIO =
    for
      echoWorkflow <- workflowStubZIO("client")
      _            <- ZIO.logInfo(s"Will submit message \"$msg\" with workflowID client-$workflowID")
      result       <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg, "client")).measureTimeConsole("getEcho")
    yield result
