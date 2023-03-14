import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import coursier.maven.MavenRepository

import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.2`
import io.github.davidgregory084.TpolecatModule
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.23`
import io.github.alexarchambault.millnativeimage.NativeImage
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.5.0`
import com.carlosedp.milldockernative.DockerNative

object versions {
  val scala3          = "3.3.0-RC3"
  val graalvm         = "graalvm-java17:22.3.1"
  val organizeimports = "0.6.0"
  val zio             = "2.0.10"
  val ziohttp         = "0.0.4+9-66d4e892-SNAPSHOT"
  val ziotemporal     = "0.1.0-RC6"
  val ziometrics      = "2.0.7"
  val ziologging      = "2.1.10"
  val idgenerator     = "1.4.0"
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
  // override def scalacOptions = T {
  //   super.scalacOptions() ++ Seq("-Wunused:all", "-Wvalue-discard")
  // }
  def forkArgs = T {
    if (sys.env.get("NATIVEGEN").nonEmpty)
      Seq("-agentlib:native-image-agent=config-merge-dir=shared/resources/META-INF/native-image")
    else Seq.empty
  }

  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:${versions.organizeimports}")
  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq("oss", "s01.oss")
      .map(r => s"https://$r.sonatype.org/content/repositories/snapshots")
      .map(MavenRepository(_))
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${versions.zio}",
    ivy"dev.zio::zio-http:${versions.ziohttp}",
    ivy"dev.zio::zio-metrics-connectors:${versions.ziometrics}",
    ivy"dev.zio::zio-logging:${versions.ziologging}",
    ivy"dev.zio::zio-logging-slf4j2-bridge:${versions.ziologging}",
    ivy"dev.vhonta::zio-temporal-core:${versions.ziotemporal}".exclude("$com.google.protobuf" -> "protobuf-java"),
    ivy"com.softwaremill.common::id-generator:${versions.idgenerator}",
  )
  object test extends Tests {
    def testFramework = T("zio.test.sbt.ZTestFramework")
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${versions.zio}",
      ivy"dev.zio::zio-test-sbt:${versions.zio}",
      ivy"dev.vhonta::zio-temporal-testkit:${versions.ziotemporal}".exclude("$com.google.protobuf" -> "protobuf-java"),
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

// The toplevel alias runner
def run(ev: eval.Evaluator, alias: String = "") = T.command {
  if (alias == "") {
    println("Use './mill run [alias]'.\nAvailable aliases:");
    aliases.foreach(x => println(x._1 + " " * (15 - x._1.length) + " - Commands: (" + x._2.mkString(", ") + ")"))
    sys.exit(1)
  }
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(
        ev,
        t.flatMap(x => x +: Seq("+")).flatMap(x => x.split(" ")).dropRight(1),
        mill.define.SelectMode.Separated,
      )(identity)
    case None => println(s"${Console.RED}ERROR:${Console.RESET} The task alias \"$alias\" does not exist.")
  }
  ()
}
