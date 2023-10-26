import Dependencies.*
import com.typesafe.sbt.packager.MappingsHelper.directory
import sbt.*
import sbt.Keys.*
import com.typesafe.sbt.packager.docker.*

ThisBuild / version      := "0.0.1"
ThisBuild / scalaVersion := "3.3.1"

(publish / skip) := true

lazy val dockerPackages: Seq[String] = Seq(
  "bash",
  "ca-certificates",
  "curl",
  "fontconfig",
  "freetype",
  "libsass",
  "libsass-dev",
  "libssl1.1",
  "libstdc++",
  "libx11",
  "libxext",
  "libxrender",
  "openjdk17-jre-headless",
  "pngquant",
  "sassc",
  "ttf-dejavu",
  "ttf-droid",
  "ttf-freefont",
  "ttf-liberation",
  "wget"
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name                             := "arttek",
    libraryDependencies ++= {
      zio ++ httpClient ++ logging ++ circe ++ mustache ++ markdown ++ sass
    },
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
  .settings(
    Universal / mappings += (Compile / packageBin).value -> "arttek.jar",
    Docker / dockerPackageMappings += file(
      s"target/scala-3.3.1/${(assembly / assemblyJarName).value}"
    )                                                    -> s"opt/docker/${(assembly / assemblyJarName).value}",
    // Custom folders
    Universal / mappings ++= directory(baseDirectory.value / "templates"),
    Universal / mappings ++= directory(baseDirectory.value / "sass"),
    Universal / mappings ++= directory(baseDirectory.value / "raw"),

    // Meta
    maintainer         := "Oto Brglez https://ogrodje.si",
    packageSummary     := "arttek",
    packageDescription := "arttek - Tool for generating art for Ogrodje Podcast",
    dockerRepository   := Some("ghcr.io"),
    dockerUsername     := Some("ogrodje"),
    dockerExposedPorts := Seq(5555),
    packageName        := "arttek",
    dockerLabels       := Map[String, String](
      "org.opencontainers.image.source" -> "github.com/ogrodje/arttek"
    ),
    dockerAliases ++= Seq(
      dockerAlias.value.withTag(Option("latest"))
    ),
    dockerCommands     := Seq(
      Cmd("FROM", "surnet/alpine-wkhtmltopdf:3.16.2-0.12.6-full as wkhtmltopdf"),
      Cmd("FROM", "alpine:3"),
      Cmd(s"RUN", s"apk add --no-cache ${dockerPackages.mkString(" ")}"),
      Cmd("EXPOSE", "5555"),
      Cmd("ENV", "PORT=5555"),
      Cmd("LABEL", s"""MAINTAINER="${maintainer.value}""""),
      Cmd("WORKDIR", (Docker / defaultLinuxInstallLocation).value),
      Cmd("COPY", "--from=wkhtmltopdf /bin/wkhtmltopdf /bin/wkhtmltopdf"),
      Cmd("COPY", "--from=wkhtmltopdf /bin/wkhtmltoimage /bin/wkhtmltoimage"),
      Cmd("COPY", "--from=wkhtmltopdf /bin/libwkhtmltox* /bin/"),
      Cmd(
        "COPY",
        s"opt/docker/${(assembly / assemblyJarName).value}",
        s"/opt/docker/${(assembly / assemblyJarName).value}"
      ),
      Cmd("COPY", s"/opt/docker/templates", s"/opt/docker/templates"),
      Cmd("COPY", s"/opt/docker/sass", s"/opt/docker/sass"),
      Cmd("COPY", s"/opt/docker/raw", s"/opt/docker/raw"),
      Cmd("ENTRYPOINT", s"""["java", "-jar", "/opt/docker/${(assembly / assemblyJarName).value}"]""")
    )
  )

scalacOptions ++= Seq(
  "-deprecation"
)
