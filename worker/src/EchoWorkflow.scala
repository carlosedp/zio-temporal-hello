package worker

import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*

import shared.EchoWorkflow

// Here is the workflow implementation that uses the activities
class EchoWorkflowImpl extends EchoWorkflow:
  private val defaultRetryOptions = ZRetryOptions.default
    .withMaximumAttempts(3)
    .withInitialInterval(300.millis)
    .withBackoffCoefficient(1)

  private val echoActivity = ZWorkflow
    .newActivityStub[EchoActivity](
      ZActivityOptions
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(defaultRetryOptions)
    )
  private val timestampActivity = ZWorkflow
    .newActivityStub[TimestampActivity](
      ZActivityOptions
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(defaultRetryOptions)
    )

  override def getEcho(msg: String, client: String = "default"): String =
    val message        = ZActivityStub.execute(echoActivity.echo(msg, client))
    val timestampedMsg = ZActivityStub.execute(timestampActivity.timestamp(message))
    timestampedMsg
end EchoWorkflowImpl
