lazy val akkaHttpVersion = "10.2.7"
lazy val akkaVersion    = "2.6.18"
lazy val pulsar4sVersion = "2.8.1"

enablePlugins(JavaAppPackaging)

mappings in (Compile, packageDoc) := Seq()


lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "net.arendsyl",
      scalaVersion    := "2.13.8"
    )),
    name := "poc-pulsar",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.10",
      "com.clever-cloud.pulsar4s" %% "pulsar4s-core"    % pulsar4sVersion,
      "com.clever-cloud.pulsar4s" %% "pulsar4s-spray-json" % pulsar4sVersion,
      "org.typelevel" %% "cats-core" % "2.7.0",
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.9"         % Test
    )
  )
