import zio.*
import zio.temporal.*
import zio.temporal.testkit.ZTestEnvironmentOptions
import zio.temporal.testkit.ZTestWorkflowEnvironment
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.test.*
import zio.test.TestAspect.*

object EchoWorkflowSpec extends ZIOSpecDefault:
  def spec = suite("Workflows")(
    test("runs echo workflow"):
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any]: activityOptions =>
        val taskQueue = TemporalQueues.echoQueue
        val sampleIn  = "Msg"
        val sampleOut = s"ACK: $sampleIn"
        for
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue)
                 @@ ZWorker.addWorkflow[EchoWorkflowImpl].fromClass
                 @@ ZWorker.addActivityImplementation(new EchoActivityImpl()(activityOptions))

          _ <- ZTestWorkflowEnvironment.setup()
          sampleWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(client =>
                              client.newWorkflowStub[EchoWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(SharedUtils.genSnowflake)
                                .withWorkflowRunTimeout(10.second)
                                .build
                            )
          result <- ZWorkflowStub.execute(sampleWorkflow.getEcho(sampleIn, "testClient"))
        yield assertTrue(result == sampleOut)
    .provideEnv
  )

  private implicit class ProvidedTestkit[E, A](thunk: Spec[ZTestWorkflowEnvironment[Any] with Scope, E]):
    def provideEnv: Spec[Scope, E] =
      thunk.provideSome[Scope](
        ZLayer.succeed(ZTestEnvironmentOptions.default),
        ZTestWorkflowEnvironment.make[Any],
      )
