package com.pinkstack.arttek
import com.pinkstack.arttek.OgrodjeClient.{getEpisode, getEpisodes}
import io.circe.*
import zio.ZIO.{logError, logErrorCause, logInfo, service, succeed}
import zio.http.*
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.model.{Method, Status}
import zio.logging.backend.SLF4J
import zio.stream.ZStream
import zio.{UIO, ZIO, *}

import java.nio.file.Paths

object ServerApp extends ZIOAppDefault:
  import SERDE.*

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val app: HttpApp[AppConfig & Client, Throwable] = Http.collectHttp[Request] { case Method.GET -> !! / "raw" / name =>
    Http.fromStream(ZStream.fromFile(Paths.get("./raw/ogrodje-white-logo.png").toFile))
  } ++ Http.collectZIO[Request] {
    case Method.GET -> !!                                =>
      succeed(Response.redirect("/episodes"))
    case Method.GET -> !! / "css" / fileName             =>
      Renderer
        .renderSASS(fileName)
        .catchAll(th => succeed(Response.text(th.getMessage)))
    case path @ (Method.GET -> !! / templateName / code) =>
      for {
        _        <- logInfo(s"Requesting: ${path}")
        response <- renderGetEpisode(templateName, code)
          .catchAllCause(c =>
            logErrorCause(c) *>
              succeed(Response.text(s"Boom - ${c}").copy(Status.InternalServerError))
          )
      } yield response
    case Method.GET -> !! / "episodes"                   =>
      getEpisodes.flatMap(episodes => Renderer.renderMustache("episodes.mustache", "episodes" -> episodes))
    case _                                               => succeed(Response.text("404").copy(status = Status.NotFound))
  }

  private def renderGetEpisode(templateName: String, code: String): ZIO[AppConfig & Client, Throwable, Response] =
    for
      server <- ZIO.service[AppConfig].map(_.server)
      out    <- getEpisode(code).flatMap { details =>
        Renderer.renderMustache(
          s"$templateName.mustache",
          "server"             -> server,
          "episode"            -> details.episode,
          "episodeSummary"     -> details.summary,
          "people"             -> details.people,
          "backgroundImageURL" -> details.backgroundImageURL,
          "showName"           -> details.showName,
          "showColor"          -> details.showColor
        )
      }
    yield out

  def run: ZIO[zio.Scope, Throwable, Unit] =
    for
      config <- AppConfig.load.map(_.copy(port = 5555))
      _      <-
        (Server.install(app).flatMap(port => logInfo(s"Server booted on port ${port}")) *> ZIO.never)
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
    yield ()
