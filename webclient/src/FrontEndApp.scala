import zio.*
import zio.http.*
import zio.http.template.*
import zio.temporal.workflow.ZWorkflowClient

import shared.MetricsApp

object FrontEndApp:
    def apply(): Routes[ZWorkflowClient, Nothing] = Routes(
        // GET /
        Method.GET / "" ->
            handler(Response.html(Dom.raw(
                """
                  |Available Endpoints:<br><br>
                  |  - <a href=/metrics>/metrics</a> - Prometheus Metrics<br>
                  |  - <a href=/echo>/echo</a> - Echo Workflow<br>
                """.stripMargin
            ))),

        // GET /echo/:msg
        Method.GET / "echo" / string("msg") ->
            handler: (msg: String, _: Request) =>
                handleMessage(msg),

        // GET /echo or /echo?msg=yourmessage
        // If a msg queryParam is provided, get the message and send it to the workflow
        // otherwise display a form to send a message
        Method.GET / "echo" ->
            handler: (req: Request) =>
                req.queryParam("msg") match
                    case Some(value) => handleMessage(value)
                    case None => ZIO.succeed(Response.html(Dom.raw(
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
                        ))),
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
            (workflowResponse, id) = response // This won't be needed in Scala 3.4
            _ <- ZIO.logDebug(s"Received message \"$workflowResponse\" from workflowID $id")
        yield Response.html(Dom.raw(s"""
                                       |Received message <b>\"$workflowResponse\"</b> from workflow.<br><br>
                                       |<a href=/echo>Send another message</a>
                    """.stripMargin))

end FrontEndApp
