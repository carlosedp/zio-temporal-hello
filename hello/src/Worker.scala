import zio.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
// import zio.temporal.protobuf.ProtobufDataConverter

val workerFactoryOptions: ULayer[ZWorkerFactoryOptions] = ZLayer.succeed {
  ZWorkerFactoryOptions.default
}
val stubOptions: ULayer[ZWorkflowServiceStubsOptions] = ZLayer.succeed {
  ZWorkflowServiceStubsOptions.default
}
val clientOptions: ULayer[ZWorkflowClientOptions] = ZLayer.succeed {
  ZWorkflowClientOptions.default
  // ZWorkflowClientOptions.default.withDataConverter(
  //   ProtobufDataConverter.makeAutoLoad(),
  // )
}

val helloWorker: URLayer[ZWorkerFactory, Unit] = ZLayer.fromZIO:
  ZIO.serviceWithZIO[ZWorkerFactory]: workerFactory =>
    for
      _      <- ZIO.logInfo("Started sample-worker")
      worker <- workerFactory.newWorker("sample-worker")
      _       = worker.addWorkflow[EchoWorkflow].from(new EchoWorkflowImpl)
    yield ()
