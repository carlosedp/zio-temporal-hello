import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*

// This is our workflow interface
@workflowInterface
trait EchoWorkflow:

    /**
     * Echoes a message back to the caller. The message could randomly fail.
     *
     * @param msg
     * @param client
     * @return
     *   the message echoed back with an ACK prefix
     */
    @workflowMethod
    def getEcho(msg: String, client: String): String

// And here the workflow implementation that uses the activity
class EchoWorkflowImpl extends EchoWorkflow:
    private val defaultRetryOptions = ZRetryOptions.default
        .withMaximumAttempts(3)
        .withInitialInterval(300.millis)
        .withBackoffCoefficient(1)

    private val echoActivity = ZWorkflow
        .newActivityStub[EchoActivity]
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(defaultRetryOptions)
        .build
    private val timestampActivity = ZWorkflow
        .newActivityStub[TimestampActivity]
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(defaultRetryOptions)
        .build

    override def getEcho(msg: String, client: String = "default"): String =
        val message        = ZActivityStub.execute(echoActivity.echo(msg, client))
        val timestampedMsg = ZActivityStub.execute(timestampActivity.timestamp(message))
        timestampedMsg
end EchoWorkflowImpl
