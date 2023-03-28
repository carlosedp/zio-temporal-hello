import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.worker.*

// TODO: This can be removed after new version of zio-temporal is released with PR #37
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.*
import io.temporal.common.converter.*

object Client:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default.withServiceUrl(
      scala.util.Properties.envOrElse("TEMPORAL_SERVER", "127.0.0.1:7233"),
    )

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default
      // TODO: Below this line can be removed once https://github.com/vitaliihonta/zio-temporal/pull/37 is merged
      .withDataConverter(
        new DefaultDataConverter(
          Seq(
            new NullPayloadConverter(),
            new ByteArrayPayloadConverter(),
            new ProtobufJsonPayloadConverter(),
            new JacksonJsonPayloadConverter(
              JsonMapper
                .builder()
                .addModule(DefaultScalaModule)
                .build(),
            ),
          )*,
        ),
      )

  val workerFactoryOptions: ULayer[ZWorkerFactoryOptions] = ZLayer.succeed:
    ZWorkerFactoryOptions.default

  def workflowStubZIO(client: String, workflowID: String) = ZIO.serviceWithZIO[ZWorkflowClient]: workflowClient =>
    workflowClient
      .newWorkflowStub[EchoWorkflow]
      .withTaskQueue(TemporalQueues.echoQueue)
      .withWorkflowId(s"$client-$workflowID")
      .withWorkflowRunTimeout(2.seconds)
      .build

  def workflowResultZIO(msg: String) =
    val workflowID = SharedUtils.genSnowflake
    val client     = "client"
    for
      echoWorkflow <- workflowStubZIO(client, workflowID)
      _            <- ZIO.logInfo(s"Will submit message \"$msg\" with workflowID $client-$workflowID")
      result <- ZWorkflowStub
                  .execute(echoWorkflow.getEcho(msg, client))
                  .measureTimeConsole("getEcho")
                  .catchSome:
                    case _: zio.temporal.TemporalClientError =>
                      ZIO.logError("Client: Exceeded retries") *> ZIO.succeed(
                        "Exceeded retries",
                      )
    yield result
