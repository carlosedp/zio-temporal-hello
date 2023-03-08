import zio.*
import zio.http.*
import zio.http.model.Method
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
      // GET /echo/:msg
      case Method.GET -> !! / "echo" / msg =>
        for
          workflowResponse <- WebClient.callEchoWorkflow(msg, "web") @@ MetricsApp.httpHitsMetric("GET", s"/echo")
          _                <- ZIO.logDebug(s"Received message \"$workflowResponse\"")
          res              <- ZIO.succeed(Response.text(workflowResponse))
        yield res

      // GET /echo
      case Method.GET -> !! / "echo" =>
        ZIO.succeed(Response.text("Send message to be echoed")) // @@ MetricsApp.httpHitsMetric("GET", "/echoblank")
