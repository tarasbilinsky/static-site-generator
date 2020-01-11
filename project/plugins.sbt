logLevel := Level.Warn

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.6")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.0")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.705",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.705"
)