import com.github.play2war.plugin._

name := """ScalaBlog"""

version := "1.0"

Play2WarPlugin.play2WarSettings

Play2WarKeys.servletVersion := "3.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "jp.t2v" %% "play2-auth"      % "0.13.1",
  "jp.t2v" %% "play2-auth-test" % "0.13.1" % "test",
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "com.typesafe.play" %% "play-mailer" % "2.4.0"
)
