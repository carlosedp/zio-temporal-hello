import zio.*
import zio.test.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.temporal.activity.*

object E2ESpec extends ZIOSpecDefault:
  def spec = suite("E2E")(
    test("Start worker and send msg via client"):
      val prog =
        for
          _              <- Worker.worker
          _              <- ZWorkflowServiceStubs.setup()
          _              <- ZWorkerFactory.setup
          workflowResult <- Client.invokeWorkflow("testmsg123")
        yield assertTrue(workflowResult == "ACK: testmsg123")
      prog.provideSome[Scope](
        ZWorkflowClientOptions.make,
        ZWorkflowClient.make,
        SharedUtils.stubOptions,
        ZWorkerFactoryOptions.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        ZActivityOptions.default,
        activityLayer,
      )
  ) @@ TestAspect.withLiveClock
    @@ TestAspect.silentLogging
    @@ TestAspect.timeout(5.seconds)
    @@ TestAspect.ignore // TODO: This test depends on a running Temporal server

  // Generate test to HTTP server and check metrics
