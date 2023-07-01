import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import coursier.Repositories

import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule
// import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.2`
// import io.github.davidgregory084.TpolecatModule
trait TpolecatModule {} // TODO: Use real mill-tpolecat once released
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.25`
import io.github.alexarchambault.millnativeimage.NativeImage
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.6.0`
import com.carlosedp.milldockernative.DockerNative
import $ivy.`com.carlosedp::mill-aliases::0.2.1`
import com.carlosedp.aliases._

object versions {
  val scala3      = "3.3.0"
  val graalvm     = "graalvm-java17:22.3.1"
  val zio         = "2.0.15"
  val ziohttp     = "3.0.0-RC2"
  val ziotemporal = "0.2.0"
  val ziometrics  = "2.0.8"
  val ziologging  = "2.1.13"
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

  object test extends ScalaTests {
    def useNativeConfig = T.input(T.env.get("NATIVECONFIG_GEN").contains("true"))
    def forkArgs = T {
      if (useNativeConfig()) Seq("-agentlib:native-image-agent=config-merge-dir=shared/resources/META-INF/native-image")
      else Seq.empty
    }
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
// Alias commands are run with: `./mill Alias/run [alias]`
// Define the alias name with the `alias` type with a sequence of tasks to be executed
object MyAliases extends Aliases {
  def lint     = alias("__.fix", "mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources")
  def fmt      = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources")
  def checkfmt = alias("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources")
  def deps     = alias("mill.scalalib.Dependency/showUpdates")
  def testall  = alias("__.test")
}
