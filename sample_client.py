import sys
import asyncio
import uuid

from temporalio.client import Client

# Use the Temporal Python SDK to call the Temporal workflow getEcho
# The workflow is defined in Scala with ZIO and it's worker polls the echo-queue
# and echoes back the messages
async def main(msg = "Hello from Python!"):
    print("Starting Temporal Python client")
    # Generate random id for the workflow
    id = str("python-client-" + str(uuid.uuid4()))
    # Connect to the Temporal server
    client = await Client.connect("localhost:7233")
    # Execute the workflow
    result = await client.execute_workflow(
        "EchoWorkflow", msg, id=id, task_queue="echo-queue"
    )

    # Print the result
    print(f"Result: {result}")

if __name__ == "__main__":
    # Read message from command line arguments
    msg = " ".join(sys.argv[1:])
    asyncio.run(main(msg))
