import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import coursier.Repositories

import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.2`
import io.github.davidgregory084.TpolecatModule
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.23`
import io.github.alexarchambault.millnativeimage.NativeImage
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.5.0`
import com.carlosedp.milldockernative.DockerNative

object versions {
  val scala3      = "3.3.0-RC4"
  val graalvm     = "graalvm-java17:22.3.1"
  val zio         = "2.0.13"
  val ziohttp     = "3.0.0-RC1"
  val ziotemporal = "0.2.0-M3"
  val ziometrics  = "2.0.8"
  val ziologging  = "2.1.12"
  val idgenerator = "1.4.0"
}

trait Common
  extends ScalaModule
  with TpolecatModule
  with ScalafmtModule
  with ScalafixModule
  with NativeImageConfig
  with DockerNative {
  def scalaVersion         = versions.scala3
  def nativeImageClassPath = runClasspath()
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Wunused:all", "-Wvalue-discard")
  }
  def useNativeConfig = T.input(T.env.get("NATIVECONFIG_GEN").contains("true"))
  def forkArgs = T {
    if (useNativeConfig()) Seq("-agentlib:native-image-agent=config-merge-dir=shared/resources/META-INF/native-image")
    else Seq.empty
  }

  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.6.0")
  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(Repositories.sonatype("snapshots"), Repositories.sonatypeS01("snapshots"))
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${versions.zio}",
    ivy"dev.zio::zio-http:${versions.ziohttp}",
    ivy"dev.zio::zio-metrics-connectors:${versions.ziometrics}",
    ivy"dev.zio::zio-logging:${versions.ziologging}",
    // ivy"dev.zio::zio-logging-slf4j2-bridge:${versions.ziologging}",
    ivy"dev.vhonta::zio-temporal-core:${versions.ziotemporal}",
    ivy"com.softwaremill.common::id-generator:${versions.idgenerator}",
  )
  object test extends Tests {
    def testFramework = T("zio.test.sbt.ZTestFramework")
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${versions.zio}",
      ivy"dev.zio::zio-test-sbt:${versions.zio}",
      ivy"dev.vhonta::zio-temporal-testkit:${versions.ziotemporal}",
    )
  }
}

trait NativeImageConfig extends NativeImage {
  def nativeImageMainClass    = "Main"
  def nativeImageGraalVmJvmId = T(versions.graalvm)
  def nativeImageOptions = super.nativeImageOptions() ++
    (if (sys.props.get("os.name").contains("Linux")) Seq("--static") else Seq.empty)
}

object shared extends Common
trait SharedCode extends ScalaModule {
  override def moduleDeps: Seq[JavaModule] = Seq(shared)
}

object worker extends Common with SharedCode with NativeImageConfig with DockerNative {
  def nativeImageName = "ziotemporalworker"
  object dockerNative extends DockerNativeConfig with NativeImageConfig {
    def nativeImageClassPath = runClasspath()
    def baseImage            = "ubuntu:22.04"
    def tags                 = List("docker.io/carlosedp/ziotemporal-worker-native")
    def exposedPorts         = Seq(8082)
  }
}

object webclient extends Common with SharedCode with NativeImageConfig with DockerNative {
  def nativeImageName = "ziotemporalwebclient"
  object dockerNative extends DockerNativeConfig with NativeImageConfig {
    def nativeImageClassPath = runClasspath()
    def baseImage            = "ubuntu:22.04"
    def tags                 = List("docker.io/carlosedp/ziotemporal-webclient-native")
    def exposedPorts         = Seq(8083)
  }
}

object client extends Common with SharedCode

// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
// Alias commands are run like `./mill run [alias]`
// Define the alias as a map element containing the alias name and a Seq with the tasks to be executed
val aliases: Map[String, Seq[String]] = Map(
  "lint"     -> Seq("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources", "__.fix"),
  "fmt"      -> Seq("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"),
  "checkfmt" -> Seq("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources"),
  "deps"     -> Seq("mill.scalalib.Dependency/showUpdates"),
  "testall"  -> Seq("__.test"),
)

def run(ev: eval.Evaluator, alias: String = "") = T.command {
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(ev, t.flatMap(x => Seq(x, "+")).flatMap(_.split("\\s+")).init, false)(identity)
    case None =>
      Console.err.println("Use './mill run [alias]'."); Console.out.println("Available aliases:")
      aliases.foreach(x => Console.out.println(s"${x._1.padTo(15, ' ')} - Commands: (${x._2.mkString(", ")})"));
      sys.exit(1)
  }
}
