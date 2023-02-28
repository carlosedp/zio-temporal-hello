import java.util.UUID

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

object WebClient:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default

  val workflowStubZIO = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
    workflowClient
      .newWorkflowStub[EchoWorkflow]
      .withTaskQueue(TemporalQueues.echoQueue)
      .withWorkflowId(s"web-${UUID.randomUUID().toString}")
      .withWorkflowRunTimeout(2.seconds)
      .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3).withBackoffCoefficient(1))
      .build

  def callEchoWorkflow(msg: String): ZIO[ZWorkflowClient, Nothing, String] =
    for
      _            <- ZIO.logDebug(s"Will submit message \"$msg\"")
      echoWorkflow <- workflowStubZIO
      result       <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg)).orElseSucceed("Error calling workflow")
    yield result
