val Http4sVersion = "0.23.27"
val CirceVersion = "0.14.9"
val MunitVersion = "1.0.0"
val LogbackVersion = "1.5.6"
val MunitCatsEffectVersion = "2.0.0"

lazy val root = (project in file("."))
  .settings(
    organization := "dopenkov",
    name := "title-crawler",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.http4s"      %% "http4s-scala-xml"    % "0.23.14",
      "io.circe"        %% "circe-generic"       % "0.14.7",
      "org.fusesource.jansi" % "jansi"           % "2.4.1",
      "co.fs2"          %% "fs2-reactive-streams" % "3.10.2",
      "org.typelevel"   %% "log4cats-slf4j"      % "2.7.0",
      "org.ccil.cowan.tagsoup" % "tagsoup"       % "1.2.1",
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect"   % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion         % Runtime,
    ),
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value.apply(x)
    }
  )

import org.typelevel.scalacoptions.ScalacOptions

Compile / tpolecatExcludeOptions += ScalacOptions.warnUnusedImports
