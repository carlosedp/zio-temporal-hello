import zio.*
import zio.temporal.*
import zio.temporal.worker.*

object Worker:
    val worker =
        ZIO.logInfo(s"Started sample-worker listening to queue ${TemporalQueues.echoQueue}")
            *> ZWorkerFactory.newWorker(TemporalQueues.echoQueue)
            @@ ZWorker.addActivityImplementationService[EchoActivity]
            @@ ZWorker.addActivityImplementationService[TimestampActivity]
            @@ ZWorker.addWorkflow[EchoWorkflowImpl].fromClass
