import java.util.UUID

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

val workflowStubZIO = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
  workflowClient
    .newWorkflowStub[EchoWorkflow]
    .withTaskQueue("echo-queue")
    .withWorkflowId(s"echo-${UUID.randomUUID().toString}")
    .withWorkflowRunTimeout(2.seconds)
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3))
    .build

val workflowResultZIO =
  for
    msg          <- ZIO.succeed("Hello there")
    echoWorkflow <- workflowStubZIO
    _            <- ZIO.logInfo(s"Will submit message \"$msg\"")
    result       <- ZWorkflowStub.execute(echoWorkflow.echo(msg))
  yield result
