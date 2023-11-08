import zio.*
import zio.test.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.testkit.*

object timestampActivitySpec extends ZIOSpecDefault:
    val spec = suite("TimestampActivity")(
        test("Timestamp message"):
            ZTestActivityEnvironment.activityOptions[Any].flatMap(implicit options =>
                val testMsg   = "testMsg"
                val sampleOut = raw"""\[\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{6}Z\] $testMsg""".r
                for
                    // Provide a "factory" method to construct the activity
                    _ <- ZTestActivityEnvironment.addActivityImplementation(new TimestampActivityImpl)
                    // Get the activity stub
                    stub <- ZTestActivityEnvironment.newActivityStub[TimestampActivity]
                        .withStartToCloseTimeout(10.second)
                        .build
                    // Invoke the activity
                    result = stub.timestamp(testMsg)
                // Assert the result
                yield assertTrue(sampleOut.matches(result))
                end for
            )
    ).provide(
        ZTestEnvironmentOptions.default,
        ZTestActivityEnvironment.make[Any],
    ) @@ TestAspect.silentLogging
end timestampActivitySpec
