import Dependencies._

ThisBuild / version      := "0.0.1"
ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name                             := "arttek",
    libraryDependencies ++= {
      zio ++ httpClient ++ circe ++ mustache ++ markdown ++ sass
    },
    mainClass                        := Some("com.pinkstack.arttek.ArttekApp"),
    Compile / packageBin / mainClass := Some("com.pinkstack.arttek.ArttekApp"),
    Compile / run / mainClass        := Some("com.pinkstack.arttek.ArttekApp")
  )
  .settings(
    assembly / mainClass             := Some("com.pinkstack.arttek.ArttekApp"),
    assembly / assemblyJarName       := "arttek.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")                        =>
        MergeStrategy.discard
      case PathList("META-INF", "jpms.args")                    =>
        MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") =>
        MergeStrategy.first
      case PathList("deriving.conf")                            =>
        MergeStrategy.last
      case PathList(ps @ _*) if ps.last endsWith ".class"       => MergeStrategy.last
      case x                                                    =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )
