import zio.*
import zio.temporal.*
import zio.temporal.worker.*

object WorkerModule:
  val worker = ZIO.logInfo(s"Started sample-worker listening to queue ${TemporalQueues.echoQueue}") *>
    ZWorkerFactory.newWorker(TemporalQueues.echoQueue) @@
    ZWorker.addActivityImplementationService[EchoActivity] @@
    ZWorker.addWorkflow[EchoWorkflowImpl].fromClass
