import zio.*
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.{Metric, MetricLabel}

// Create the Prometheus router exposing metrics in "/metrics" and incrementing a counter
object MetricsApp:
  // Sample metrics
  def httpHitsMetric(method: String, path: String) =
    Metric
      .counter("httphits")
      .fromConst(1)
      .tagged(MetricLabel("method", method), MetricLabel("path", path))

  def echoActivityCall(client: String) =
    Metric
      .counter("echoactivitycall")
      .fromConst(1)
      .tagged(MetricLabel("client", client))

  // Map calls to "/greet/some_name" to "/greet/:person" for metric aggregation
  def pathLabelMapper: PartialFunction[Request, String] =
    case Method.GET -> Root / _ => "/"

  def apply(): Http[PrometheusPublisher, Nothing, Request, Response] =
    Http.collectZIO[Request]:
      case Method.GET -> Root / "metrics" =>
        ZIO.serviceWithZIO[PrometheusPublisher](
          _.get.map(Response.text)
        ) @@ MetricsApp.httpHitsMetric("GET", "/metrics")
