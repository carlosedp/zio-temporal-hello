import zio.*
import zio.temporal.*
import zio.temporal.testkit.ZTestEnvironmentOptions
import zio.temporal.testkit.ZTestWorkflowEnvironment
import zio.temporal.worker.ZWorkerOptions
import zio.temporal.workflow.*
import zio.test.*
import zio.test.TestAspect.*

object EchoWorkflowSpec extends ZIOSpecDefault:
  private implicit class ProvidedTestkit[E, A](thunk: Spec[ZTestWorkflowEnvironment[Any], E]):
    def provideEnv: Spec[Any, E] =
      thunk.provide(
        ZLayer.succeed(ZTestEnvironmentOptions.default),
        ZTestWorkflowEnvironment.make[Any],
      )
  private def withWorkflow[R, E, A](f: ZIO[R, TemporalError[E], A]): RIO[R, A] =
    f.mapError(e => new RuntimeException(s"IO failed with $e"))

  def spec = suite("Workflows")(
    test("runs echo workflow") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = TemporalQueues.echoQueue
        val sampleIn  = "Msg"
        val sampleOut = s"ACK: $sampleIn"
        val client    = testEnv.workflowClient

        import testEnv.activityOptions
        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[EchoWorkflowImpl]
          .fromClass
          .addActivityImplementation(new EchoActivityImpl())

        withWorkflow {
          testEnv.use() {
            for
              echoWorkflow <- client
                                .newWorkflowStub[EchoWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(SharedUtils.genSnowflake)
                                .withWorkflowRunTimeout(10.seconds)
                                .build
              result <- ZWorkflowStub.execute(echoWorkflow.getEcho(sampleIn, "test"))
            yield assertTrue(result == sampleOut)
          }
        }
      }
    }.provideEnv,
  )
