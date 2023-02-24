import java.util.UUID

import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._

val workflowStubZIO = ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient
    .newWorkflowStub[EchoWorkflow]
    .withTaskQueue("echo-queue")
    .withWorkflowId("echo-" + UUID.randomUUID().toString)
    .withWorkflowRunTimeout(2.seconds)
    .withRetryOptions(
      ZRetryOptions.default.withMaximumAttempts(3),
    )
    .build
}

val msg = "Hello there"
val workflowResultZIO =
  for
    echoWorkflow <- workflowStubZIO
    _            <- ZIO.logInfo(s"Will submit message \"$msg\"")
    result       <- ZWorkflowStub.execute(echoWorkflow.echo(msg))
  yield result
