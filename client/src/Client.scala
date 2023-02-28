import java.util.UUID

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.worker.*

val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
  ZWorkflowServiceStubsOptions.default

val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
  ZWorkflowClientOptions.default

val workerFactoryOptions: ULayer[ZWorkerFactoryOptions] = ZLayer.succeed:
  ZWorkerFactoryOptions.default

def workflowStubZIO(client: String) = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
  workflowClient
    .newWorkflowStub[EchoWorkflow]
    .withTaskQueue(TemporalQueues.echoQueue)
    .withWorkflowId(s"$client-${UUID.randomUUID().toString}")
    .withWorkflowRunTimeout(2.seconds)
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3).withBackoffCoefficient(1))
    .build

val workflowResultZIO =
  for
    msg          <- ZIO.succeed("Hello there")
    echoWorkflow <- workflowStubZIO("client")
    _            <- ZIO.logInfo(s"Will submit message \"$msg\"")
    result       <- ZWorkflowStub.execute(echoWorkflow.getEcho(msg, "client")).measureTimeConsole("getEcho")
  yield result
