organization := "edu.cmu.spf"
name         := "slio"

scalaVersion in ThisBuild := "2.11.11"
sbtVersion in Global := "0.13.16"

assemblyMergeStrategy in assembly := {
  case x => MergeStrategy.discard
}
assemblyJarName in assembly := "bigdata-labels.jar"

lazy val commonSettings = Seq(
  version      := "0.1.0"
)

lazy val slio = (project in file("."))
  .settings(commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark"       %% "spark-core"               % "2.1.0",
      "org.apache.spark"       %% "spark-mllib"              % "2.1.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
      "org.scalaz"             %% "scalaz-core"              % "7.1.4",
      "org.scalatest"          %% "scalatest"                % "3.0.4",
      "org.typelevel"          %% "cats-core"                % "1.0.1",
      "org.typelevel"          %% "cats-mtl-core"            % "0.2.1"
    )
  )
