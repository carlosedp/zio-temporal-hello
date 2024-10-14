package shared

import zio.*
import zio.http.*
import zio.metrics.MetricKeyType.Counter as CounterKeyType
import zio.metrics.MetricState.Counter
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.{Metric, MetricLabel}

// Create the Prometheus router exposing metrics in "/metrics" and incrementing a counter
object MetricsApp:
  // Sample metrics
  def httpHitsMetric(method: String, path: String): Metric[CounterKeyType, Any, Counter] =
    Metric
      .counter("httphits")
      .fromConst(1)
      .tagged(MetricLabel("method", method), MetricLabel("path", path))

  def echoActivityCall(client: String): Metric[CounterKeyType, Any, Counter] =
    Metric
      .counter("echoactivitycall")
      .fromConst(1)
      .tagged(MetricLabel("client", client))

  // Define the routes for the metrics publisher
  def apply(): Routes[PrometheusPublisher, Nothing] =
    Routes(
      Method.GET / "metrics" -> handler(
        ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
          @@ MetricsApp.httpHitsMetric("GET", "/metrics")
      )
    ) @@ Middleware.metrics()

end MetricsApp
