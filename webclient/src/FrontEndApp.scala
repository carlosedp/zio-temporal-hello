import zio.*
import zio.http.*
import zio.temporal.*
import zio.temporal.workflow.*

/**
 * An http app that:
 *   - Accepts a `Request` and returns a `Response`
 *   - Does not fail
 *   - Does not use the environment
 */
object FrontEndApp:
    def apply(): Http[ZWorkflowClient, Nothing, Request, Response] =
        Http.collectZIO[Request]:
            // GET /
            case Method.GET -> Root =>
                ZIO.succeed(
                    Response.html(
                        """
                          |Available Endpoints:<br><br>
                          |  - <a href=/metrics>/metrics</a> - Prometheus Metrics<br>
                          |  - <a href=/echo>/echo</a> - Echo Workflow<br>
                        """.stripMargin
                    )
                )

            // GET /echo/:msg
            case req @ Method.GET -> Root / "echo" / msg =>
                handleMessage(msg)

            // GET /echo or /echo?msg=yourmessage
            // If a msg queryParam is provided, get the message and send it to the workflow
            // otherwise display a form to send a message
            case req @ Method.GET -> Root / "echo" =>
                if req.url.queryParams.nonEmpty then
                    val msg = req.url.queryParams.get("msg").get.asString
                    handleMessage(msg)
                else
                    ZIO.succeed(
                        Response.html(
                            """
                              |<html>
                              |Send message to be echoed using <a href=/echo/yourmessage>/echo/yourmessage</a> or the form below.
                              |<br><br>
                              |<form action="/echo" method="get">
                              |<input type="text" name="msg" value="Hello World!">
                              |<input type="submit" value="Submit">
                              |</form>
                              |</html>
                            """.stripMargin
                        )
                    )

    /**
     * Send a message to the echo workflow and return the response
     * @param msg
     *   The message to send to the workflow
     * @return
     *   The response from the workflow
     */
    def handleMessage(msg: String): ZIO[ZWorkflowClient, Nothing, Response] =
        for
            response <- WebClient.invokeWorkflow(msg) @@ MetricsApp.httpHitsMetric("GET", "/echo")
            (workflowResponse, id) = response
            _ <- ZIO.logDebug(s"Received message \"$workflowResponse\" from workflowID $id")
            res <- ZIO.succeed(Response.html(
                s"""
                   |Received message <b>\"$workflowResponse\"</b> from workflow.<br><br>
                   |<a href=/echo>Send another message</a>
                """.stripMargin
            ))
        yield res
end FrontEndApp
