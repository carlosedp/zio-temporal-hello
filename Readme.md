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

The app have a two projects, one called [`worker`](./worker/src/) which is the worker that attaches and polls the Temporal queue and a project called [`client`](./client/src/) that generates the calls to the Temporal workflows.

Since the workflows and activities are shared between client and worker, it's code is in the  [`shared`](./shared/src/) directory.

The app uses `zio-http` to publish Prometheus metrics from `zio-metrics` library.

For a simple app with only workflows and integrated worker/client, check the [simple](https://github.com/carlosedp/zio-temporal-hello/tree/simple) tag. This is similar to the example from Temporal Java [tutorial](https://learn.temporal.io/getting_started/java/hello_world_in_java).

## Usage

Start the Temporal full stack using the provided docker-compose file:

```sh
docker-compose up -d

# To follow the temporal stack logs use
docker-compose logs -f
```

or use [Temporalite](https://github.com/temporalio/temporalite), an "all-in-one" binary for Temporal development and testing which is much lighter on resources:

```sh
# Download latest version from https://github.com/temporalio/temporalite/releases/latest for your platform
# Unpack and run the binary on another terminal
./temporalite start --namespace default
```

Run the workflow worker with:

```sh
./mill worker.run
```

And on another shell, run the client with:

```sh
./mill client.run
```

Watch the logs and follow the workflow using the Temporal UI at [http://localhost:8233](http://localhost:8233).

The worker publishes Prometheus metrics at [http://localhost:8082/metrics](http://localhost:8082/metrics).

