import zio.*
import zio.test.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.testkit.*

object timestampActivitySpec extends ZIOSpecDefault:
    val spec = suite("TimestampActivity")(
        test("Timestamp message"):
            ZTestActivityEnvironment.activityRunOptions[Any].flatMap(implicit options =>
                val testMsg = "testMsg"
                val sampleOut =
                    raw"""\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]+Z\] $testMsg""".r
                for
                    // Provide a "factory" method to construct the activity
                    _ <- ZTestActivityEnvironment.addActivityImplementation(new TimestampActivityImpl)
                    // Get the activity stub
                    stub <- ZTestActivityEnvironment.newActivityStub[TimestampActivity](
                        ZActivityOptions
                            .withStartToCloseTimeout(10.second)
                    )
                    // Invoke the activity
                    result = stub.timestamp(testMsg)
                // Assert the result
                yield assertTrue(sampleOut.matches(result))
            )
    ).provide(
        ZTestEnvironmentOptions.default,
        ZTestActivityEnvironment.make[Any],
    ) @@ TestAspect.silentLogging
end timestampActivitySpec
