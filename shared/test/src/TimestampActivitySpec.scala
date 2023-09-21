import zio.*
import zio.test.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.testkit.*

object timestampActivitySpec extends ZIOSpecDefault:
  val spec = suite("TimestampActivity")(
    test("Timestamp message"):
      ZTestActivityEnvironment.activityOptions[Any].flatMap(implicit options =>
        val sampleOut = """\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{6}Z\] ACK: testMsg""".r
        for
          // Provide a "factory" method to construct the activity
          _ <- ZTestActivityEnvironment.addActivityImplementation(new TimestampActivityImpl)
          // Get the activity stub
          stub <- ZTestActivityEnvironment.newActivityStub[TimestampActivity]
            .withStartToCloseTimeout(10.second)
            .build
          // Invoke the activity
          result = stub.timestamp("ACK: testMsg")
        // Assert the result
        yield assertTrue(sampleOut.matches(result))
      )
  ).provide(
    ZTestEnvironmentOptions.default,
    ZTestActivityEnvironment.make[Any],
  ) @@ TestAspect.withLiveClock @@ TestAspect.silentLogging
