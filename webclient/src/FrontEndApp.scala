import zio.*
import zio.http.*
import zio.http.Method
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
        ZIO.succeed(Response.html(s"""
          Available Endpoints:<br><br>
            - <a href=/metrics>/metrics</a> - Prometheus Metrics<br>
            - <a href=/echo>/echo</a> - Echo Workflow<br>
        """.stripMargin))
      // GET /echo/:msg
      case Method.GET -> Root / "echo" / msg =>
        for
          workflowResponse <- WebClient.invokeWorkflow(msg) @@ MetricsApp.httpHitsMetric("GET", s"/echo")
          _                <- ZIO.logDebug(s"Received message \"$workflowResponse\"")
          res              <- ZIO.succeed(Response.text(workflowResponse))
        yield res

      // GET /echo
      case Method.GET -> Root / "echo" =>
        ZIO.succeed(
          Response.text("Send message to be echoed as /echo/yourmessage")
        )
