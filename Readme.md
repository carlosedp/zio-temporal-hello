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

or use [Temporal cli](https://github.com/temporalio/cli) (recommended for development), an "all-in-one" binary for Temporal development and testing which is much lighter on resources:

```sh
# Install with:
curl -sSf https://temporal.download/cli.sh | sh

# and run the dev server listening on all IPs (by default, it listens on localhost only)
$HOME/.temporalio/bin/temporal server start-dev --ip 0.0.0.0
```

Run the workflow worker with:

```sh
./mill worker.run
```

And on another shell, run the CLI client with:

```sh
./mill client.run "My Message"
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
??? curl http://localhost:8083/echo/testmsg
ACK: testmsg%
```

Another option is using [Temporal.io tctl](https://github.com/temporalio/tctl) command line utility which can among many other functions, start and observe a workflow:

```sh
??? tctl workflow start --workflow_type EchoWorkflow --taskqueue echo-queue --workflow_id testID01 --input '"CLI Msg"'
Started Workflow Id: testID01, run Id: dcb60763-f14b-4763-9a36-7a08423214ad

??? tctl workflow observe --workflow_id testID01
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

### Generating module binaries (GraalVM Native-Image)

The build can also generate native image binaries for almost instant startup and resource consumption. To generate the binaries, do:

```sh
# For the worker:
./mill show worker.nativeImage

# The binary name will be printed at the end:
...
Produced artifacts:
 /home/carlosedp/repos/zio-temporal-hello/out/worker/nativeImage.dest/ziotemporalworker (executable)
 /home/carlosedp/repos/zio-temporal-hello/out/worker/nativeImage.dest/ziotemporalworker.build_artifacts.txt (txt)
========================================================================================================================
Finished generating 'ziotemporalworker' in 3m 47s.
"ref:362fb41c:/home/carlosedp/repos/zio-temporal-hello/out/worker/nativeImage.dest/ziotemporalworker"

# For the webclient:
./mill show webclient.nativeImage

# The binary name will be printed at the end:
...
Finished generating 'ziotemporalwebclient' in 4m 26s.
"ref:3b33c0f0:/home/carlosedp/repos/zio-temporal-hello/out/webclient/nativeImage.dest/ziotemporalwebclient"
```

### Generating Docker images with binary (GraalVM Native-Image)

It's also possible to generate a Docker image containing the binary for cloud deployment. To generate the images, do:

```sh
# For the worker:
./mill worker.dockerNative.build

# The image name will be printed:
...
#7 exporting layers 0.5s done
#7 writing image sha256:b2711446b07e8a89c62c8a4b886652d006e328715e8cdb4e3c2ea9e4014dc92d done
#7 naming to docker.io/carlosedp/ziotemporal-worker-native done
#7 DONE 0.5s

# For the webclient:
./mill webclient.dockerNative.build

# The image name will be printed:
...
#7 exporting layers 0.9s done
#7 writing image sha256:d30fcd5dbb115e015be878c4eff363d95d13c00027c41292a386fe7dbba5f037 done
#7 naming to docker.io/carlosedp/ziotemporal-webclient-native done
#7 DONE 0.9s
```

Run the images with:

```sh
# Running the worker (Temporal or Temporalite should be running too on another shell)
# Use your temporal server IP address in the environment variable below
docker run -d -e TEMPORAL_SERVER="192.168.1.10:7233" -p 8082:8082 --name ziotemporalite-worker docker.io/carlosedp/ziotemporal-worker-native

# Follow the logs with
docker logs -f ziotemporalite-worker

# Run in a similar way the webclient
docker run -d -e TEMPORAL_SERVER="192.168.1.10:7233" -p 8083:8083 --name ziotemporalite-webclient docker.io/carlosedp/ziotemporal-webclient-native

# Follow the logs with
docker logs -f ziotemporalite-webclient
```

And generate requests like the previous section, via web, `tctl` cli or the client module. Eg. `curl http://192.168.1.10:8083/echo/testmsg`.

## Generating GraalVM Native Image config

GraalVM Native image requires reflected and proxied classes to be declared beforehand. This is is eased by the native-image-agent which can be run for each application using the `NATIVEGEN=true` environment variable in the `run` task. This appends to the configs in [./shared/resources/META-INF/native-image](./shared/resources/META-INF/native-image).

After using new libraries that might require update, run each module as `NATIVEGEN=true ./mill worker.run`, `NATIVEGEN=true ./mill webclient.run`, etc to regenerate the config files.

Initialization arguments which go into [native-image.properties](./shared/resources/META-INF/native-image/native-image.properties) are not generated automatically.
