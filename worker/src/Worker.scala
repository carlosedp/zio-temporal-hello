import zio.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*

object WorkerModule:
  val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed:
    ZWorkflowServiceStubsOptions.default.withServiceUrl(
      scala.util.Properties.envOrElse("TEMPORAL_SERVER", "127.0.0.1:7233"),
    )

  val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed:
    ZWorkflowClientOptions.default

  val workerFactoryOptions: ULayer[ZWorkerFactoryOptions] = ZLayer.succeed:
    ZWorkerFactoryOptions.default

  val worker: URLayer[EchoActivity with ZWorkerFactory, Unit] = ZLayer.fromZIO:
    ZIO.serviceWithZIO[ZWorkerFactory]: workerFactory =>
      for
        worker       <- workerFactory.newWorker(TemporalQueues.echoQueue)
        _            <- ZIO.logInfo(s"Started sample-worker listening to queue ${TemporalQueues.echoQueue}")
        activityImpl <- ZIO.service[EchoActivity]
        _             = worker.addActivityImplementation(activityImpl)
        _             = worker.addWorkflow[EchoWorkflow].from(new EchoWorkflowImpl)
      yield ()
