ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.0-RC3"

lazy val root = (project in file("."))
  .settings(
    name := "zio-temporal-hello",
  )

lazy val worker = project
  .in(file("worker"))
  .settings(
    name := "worker",
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    // resolvers +=
    //   "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    Compile / scalaSource := baseDirectory.value / "worker" / "src",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "shared" / "src",
    libraryDependencies ++= {
      val zioTemporalVersion = "0.1.0-RC6"
      val zioVersion         = "2.0.10"
      val zioMetrics         = "2.0.7"
      val zioLoggingVersion  = "2.1.10"
      val zioHttpVersion     = "0.0.4+9-66d4e892-SNAPSHOT"

      val zioTemporal = Seq(
        "dev.vhonta" %% "zio-temporal-core"    % zioTemporalVersion,
        "dev.vhonta" %% "zio-temporal-testkit" % zioTemporalVersion % Test,
      )

      val zioDeps = Seq(
        "dev.zio" %% "zio"                    % zioVersion,
        "dev.zio" %% "zio-metrics-connectors" % zioMetrics,
        "dev.zio" %% "zio-http"               % zioHttpVersion,
        "dev.zio" %% "zio-logging"            % zioLoggingVersion,
      )

      val miscDeps = Seq(
        "ch.qos.logback"           % "logback-classic" % "1.4.5",
        "com.softwaremill.common" %% "id-generator"    % "1.4.0",
      )

      zioDeps ++ zioTemporal ++ miscDeps
    },
  )
