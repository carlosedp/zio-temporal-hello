import zio.*
import zio.temporal.*
import zio.temporal.testkit.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.test.*
import zio.test.TestAspect.*

object EchoWorkflowSpec extends ZIOSpecDefault:
  def spec = suite("Workflows")(
    test("runs echo workflow"):
      ZTestWorkflowEnvironment.activityOptions[Any].flatMap(implicit options =>
        val taskQueue = TemporalQueues.echoQueue
        val sampleIn  = "Msg"
        val sampleOut = """\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{6}Z\] ACK: Msg""".r
        for
          // Create the worker
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue)
            @@ ZWorker.addWorkflow[EchoWorkflowImpl].fromClass
            @@ ZWorker.addActivityImplementation(new EchoActivityImpl())
            @@ ZWorker.addActivityImplementation(new TimestampActivityImpl())
          // Setup the workflow test environment
          _ <- ZTestWorkflowEnvironment.setup()
          // Create the workflow stub
          echoWorkflow <- ZTestWorkflowEnvironment
            .newWorkflowStub[EchoWorkflow]
            .withTaskQueue(taskQueue)
            .withWorkflowId(SharedUtils.genSnowflake)
            // Set workflow timeout
            .withWorkflowRunTimeout(10.second)
            .build
          result <- ZWorkflowStub.execute(echoWorkflow.getEcho(sampleIn, "testClient"))
        yield assertTrue(sampleOut.matches(result))
      )
  ).provideSome[Scope](
    ZTestEnvironmentOptions.default,
    ZTestWorkflowEnvironment.make[Any],
  ) @@ TestAspect.withLiveClock @@ TestAspect.silentLogging
