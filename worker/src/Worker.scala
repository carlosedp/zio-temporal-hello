package worker

import zio.*
import zio.temporal.*
import zio.temporal.worker.*

import shared.TemporalQueues

object Worker:
  val worker: ZIO[ZWorkerFactory & EchoActivity & TimestampActivity, Nothing, ZWorker] =
    ZIO.logInfo(s"Started sample-worker listening to queue ${TemporalQueues.echoQueue}")
      *> ZWorkerFactory.newWorker(TemporalQueues.echoQueue)
      @@ ZWorker.addActivityImplementationService[EchoActivity]
      @@ ZWorker.addActivityImplementationService[TimestampActivity]
      @@ ZWorker.addWorkflow[EchoWorkflowImpl].fromClass
