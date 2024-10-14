package worker

import zio.*
import zio.test.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.testkit.*

object echoActivitySpec extends ZIOSpecDefault:
  val spec = suite("EchoActivity"):
    test("Echo message"):
      ZTestActivityEnvironment.activityRunOptions[Any].flatMap(implicit options =>
        for
          // Provide a "factory" method to construct the activity
          _ <- ZTestActivityEnvironment.addActivityImplementation(new EchoActivityImpl)
          // Get the activity stub
          stub <- ZTestActivityEnvironment.newActivityStub[EchoActivity](
            ZActivityOptions
              .withStartToCloseTimeout(10.second)
          )
          // Invoke the activity
          result = stub.echo("testMsg", "testClient")
        // Assert the result
        yield assertTrue(result == "ACK: testMsg")
      )
  .provide(
    ZTestEnvironmentOptions.default,
    ZTestActivityEnvironment.make[Any],
  ) @@ TestAspect.silentLogging
end echoActivitySpec
