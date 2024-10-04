package shared

import zio.temporal.*

// This is our workflow interface
@workflowInterface
trait EchoWorkflowInterface:

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
