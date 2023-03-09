# ZIO - Temporal Sample Application

This is a sample "hello" app written in Scala 3, [ZIO Functional Framework](https://zio.dev/) and the [Temporal](https://temporal.io/) platform.

The idea is to set a modern template project using the latest versions of the following libraries:

- [Scala 3](https://docs.scala-lang.org/scala3/new-in-scala3.html)
- [Mill build tool](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html)
- [Temporal](https://github.com/temporalio/temporal)
- [ZIO 2](https://github.com/zio/zio)
- [zio-temporal](https://github.com/vitaliihonta/zio-temporal)
- [zio-logging](https://zio.dev/ecosystem/officials/zio-logging/)
- [zio-metrics](https://zio.dev/ecosystem/officials/zio-metrics/)
- [zio-http](https://github.com/zio/zio-http)

## Organization

The app have multiple modules:

The main module is the [`worker`](./worker/src/) which runs the workers that executes the workflows and activities attaching and polling the Temporal queue.

One is a command line (CLI) [`client`](./client/src/) that when executed, generates the call to the Temporal workflow.

Another one is a [`webclient`](./webclient/src/) that starts an HTTP server listening to REST API and proviidng calls to the Temporal workflows.

Since the workflows and activities are shared between the clients and worker, it's code is in the [`shared`](./shared/src/) directory.

The app uses `zio-http` to publish Prometheus metrics from `zio-metrics` library.

For a simple app with only workflows and integrated worker/client, check the [simple](https://github.com/carlosedp/zio-temporal-hello/tree/simple) tag. This is similar to the example from Temporal Java [tutorial](https://learn.temporal.io/getting_started/java/hello_world_in_java).

## Usage

Start the Temporal full stack using the provided docker-compose file:

```sh
docker-compose up -d

# To follow the temporal stack logs use
docker-compose logs -f
```

or use [Temporalite](https://github.com/temporalio/temporalite) (recommended for development), an "all-in-one" binary for Temporal development and testing which is much lighter on resources:

```sh
# Download latest version from https://github.com/temporalio/temporalite/releases/latest for your platform
# Unpack and run the binary on another terminal
./temporalite start --namespace default
```

Run the workflow worker with:

```sh
./mill worker.run
```

And on another shell, run the CLI client with:

```sh
./mill client.run
```

To start the Web client, run:

```sh
./mill webclient.run

# or during development to watch for changes and re-run, do:
./mill -w webclient.runBackground

# this can be done for developing any module
```

Then, submit an API request to the `echo` path which will be sent to the Temporal cluster and returned:

```sh
❯ curl http://localhost:8083/echo/testmsg
ACK: testmsg%
```

Another option is using [Temporal.io tctl](https://github.com/temporalio/tctl) command line utility which can among many other functions, start and observe a workflow:

```sh
❯ tctl workflow start --workflow_type EchoWorkflow --taskqueue echo-queue --workflow_id testID01 --input '"CLI Msg"'
Started Workflow Id: testID01, run Id: dcb60763-f14b-4763-9a36-7a08423214ad

❯ tctl workflow observe --workflow_id testID01
Progress:
  1, 2023-03-09T14:36:09Z, WorkflowExecutionStarted
  2, 2023-03-09T14:36:09Z, WorkflowTaskScheduled
  3, 2023-03-09T14:36:09Z, WorkflowTaskStarted
  4, 2023-03-09T14:36:09Z, WorkflowTaskCompleted
  5, 2023-03-09T14:36:09Z, ActivityTaskScheduled
  6, 2023-03-09T14:36:09Z, ActivityTaskStarted
  7, 2023-03-09T14:36:09Z, ActivityTaskCompleted
  8, 2023-03-09T14:36:09Z, WorkflowTaskScheduled
  9, 2023-03-09T14:36:09Z, WorkflowTaskStarted
  10, 2023-03-09T14:36:09Z, WorkflowTaskCompleted
  11, 2023-03-09T14:36:09Z, WorkflowExecutionCompleted

Result:
  Run Time: 1 seconds
  Status: COMPLETED
  Output: ["ACK: CLI Msg"]
```

Watch the logs and follow the workflow using the Temporal UI at [http://localhost:8233](http://localhost:8233).

The worker publishes Prometheus metrics at [http://localhost:8082/metrics](http://localhost:8082/metrics).
