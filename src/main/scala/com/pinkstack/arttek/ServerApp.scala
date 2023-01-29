package com.pinkstack.arttek
import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}
import com.pinkstack.arttek.OgrodjeClient.{getEpisode, getEpisodes, EpisodeWithDetails}
import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import io.circe.generic.semiauto.*
import io.circe.*
import zio.Console.printLine
import zio.ZIO.{fail, fromEither, fromOption, logDebug, logInfo, service, succeed}
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.model.Status.Ok
import zio.http.model.{Headers, Method, Status}
import zio.http.*
import zio.logging.backend.SLF4J
import zio.stream.{ZPipeline, ZStream}
import zio.{UIO, ZIO, *}

import java.io.{IOException, StringWriter}
import java.nio.file.{Path, Paths}
import java.util
import scala.jdk.CollectionConverters.*

object ServerApp extends ZIOAppDefault:
  import SERDE.*

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val app: HttpApp[AppConfig & Client, Throwable] =
    Http
      .collectHttp[Request] { case Method.GET -> !! / "raw" / name =>
        Http.fromStream(ZStream.fromFile(Paths.get("./raw/ogrodje-white-logo.png").toFile))
      } ++ Http.collectZIO[Request] {
      case Method.GET -> !! / "css" / fileName    =>
        Renderer
          .renderSASS(fileName)
          .catchAll(th => succeed(Response.text(th.getMessage)))
      case Method.GET -> !! / templateName / code =>
        for {
          server   <- ZIO.service[AppConfig].map(_.server)
          response <- getEpisode(code).flatMap { details =>
            Renderer.renderMustache(
              s"$templateName.mustache",
              "server"             -> server,
              "episode"            -> details.episode,
              "episodeSummary"     -> details.summary,
              "people"             -> details.people,
              "backgroundImageURL" -> details.backgroundImageURL
            )
          }.catchAll(th => succeed(Response.text(th.getMessage)))
        } yield response
      case Method.GET -> !! / "episodes"          =>
        getEpisodes.flatMap { episodes =>
          Renderer.renderMustache("episodes.mustache", "episodes" -> episodes)
        }
      case _                                      =>
        ZIO.succeed(Response.text("404"))
    }

  def run =
    for {
      config <- AppConfig.load.map(_.copy(port = 5555))
      _      <- (Server.install(app).flatMap(port => logInfo(s"Server booted on port ${port}")) *> ZIO.never)
        .provide(
          Scope.default,
          Client.default,
          ZLayer.fromZIO(succeed(config)),
          ServerConfig.live(
            ServerConfig.default
              .port(config.port)
              .leakDetection(LeakDetectionLevel.SIMPLE)
          ),
          Server.live
        )
    } yield ()
