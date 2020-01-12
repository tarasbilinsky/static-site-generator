import play.sbt.{PlayInternalKeys, PlayRunHook}
import play.sbt.run.PlayRun

name := "template"
organization := "com.intteh"
version := "0.0.1"

lazy val `template` = (project in file(".")).enablePlugins(PlayScala)

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Maven central"  at "https://repo1.maven.org/maven2",
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.705",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.softwaremill.macwire" %% "macros" % "2.3.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)

scalaVersion := "2.13.1"
scalacOptions ++= Seq("-feature","-unchecked","-deprecation") //"-Ylog-classpath","-Xlog-implicits"
Compile / javacOptions ++= Seq("-Xlint:unchecked","-Xlint:deprecation")

//sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value

Compile / doc / sources := Seq.empty
Compile / packageDoc / publishArtifact := false

TwirlKeys.templateImports in Compile ++= Seq(
)

// Assets / LessKeys.less / includeFilter := "*.less"
// Assets / LessKeys.less / excludeFilter := "_*.less"


val wGetDir = settingKey[File]("WGet dir")
wGetDir := new File(target.value + "/wget")

val s3TargetBucket = settingKey[String]("s3 bucket")
s3TargetBucket := "www.roofmacs.com"

val runAndPublish = inputKey[Unit]("")
val publishHook = taskKey[Seq[PlayRunHook]]("")
publishHook := Seq(WGet((Compile / sources).value, wGetDir.value, s3TargetBucket.value, gzip = true, streams.value.log))
runAndPublish in Compile := PlayRun.playRunTask(publishHook, PlayInternalKeys.playDependencyClasspath, PlayInternalKeys.playReloaderClasspath, PlayInternalKeys.playAssetsClassLoader).evaluated


      