package e2etest

import zio.*
import zio.test.*
import zio.temporal.worker.{ZWorkerFactoryOptions, ZWorkerFactory}
import zio.temporal.workflow.*
import zio.temporal.activity.*

import shared.SharedUtils
import client.Client
import worker.{Worker, echoActivityLayer, timestampActivityLayer}

object E2ESpec extends ZIOSpecDefault:

    // Check if the temporal server is running before running the test
    val isPortOpen = SharedUtils.temporalServer.split(":") match
        case Array(host, port) => E2ETestUtils.isPortOpen(host, port.toInt)
        case _                 => false
    val ignoreTest =
        if isPortOpen then TestAspect.identity else TestAspect.ignore // TODO: Check if the temporal server is running

    def spec = suite("E2E"):
        test("Start worker and send msg via client"):
            val sampleOut = """\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{6}Z\] ACK: testmsg123""".r
            val prog =
                for
                    _              <- Worker.worker
                    _              <- ZWorkflowServiceStubs.setup()
                    _              <- ZWorkerFactory.setup
                    workflowResult <- Client.invokeWorkflow("testmsg123")
                yield assertTrue(sampleOut.matches(workflowResult))

            prog.provideSome[Scope](
                ZWorkflowClientOptions.make,
                ZWorkflowClient.make,
                SharedUtils.stubOptions,
                ZWorkerFactoryOptions.make,
                ZWorkflowServiceStubs.make,
                ZWorkerFactory.make,
                ZActivityRunOptions.default,
                echoActivityLayer,
                timestampActivityLayer,
            )
    @@ TestAspect.silentLogging
        @@ TestAspect.timeout(5.seconds)
        @@ ignoreTest

    // Generate test to HTTP server and check metrics
end E2ESpec
