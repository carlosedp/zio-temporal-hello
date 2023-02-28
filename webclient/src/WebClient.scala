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
      .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3).withBackoffCoefficient(0.5))
      .build

  def callEchoWorkflow(msg: String) =
    for
      _            <- ZIO.logInfo(s"Will submit message \"$msg\"")
      echoWorkflow <- workflowStubZIO
      result       <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg)).orElse(ZIO.succeed("Error calling workflow"))
    yield result
