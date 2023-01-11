package com.pinkstack.arttek
import io.circe.{DecodingFailure, Json}
import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import zio.*
import zio.http.{Body, Client, Http, HttpApp, Request, URL, *}
import zio.Console.printLine
import zio.{UIO, ZIO}
import zio.ZIO.{fail, fromEither, fromOption, service, succeed}
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.model.{Headers, Method, Status}
import zio.stream.{ZPipeline, ZStream}

import java.io.StringWriter
import java.util
// import liqp.Template
import zio.http.model.Status.Ok
import io.circe.*
import io.circe.generic.semiauto.*
import java.io.IOException
import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*
import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}

object RendererApp extends ZIOAppDefault:
  import SERDE.*

  def getEpisode(code: String): ZIO[AppConfig & Client, Throwable, (Episode, String)] =
    for
      episode        <- OgrodjeClient
        .episode(code)
        .flatMap(j => fromOption(j.hcursor.downField("episode").focus))
        .flatMap(j => fromEither(j.as[SERDE.Episode]))
        .catchAll(th => ZIO.fail(new RuntimeException(th.toString)))
      episodeSummary <- Render.renderMarkdown(episode.summary)
    yield (episode, episodeSummary)

  def getEpisodes: ZIO[AppConfig & Client, Throwable, Array[Episode]] =
    OgrodjeClient.episodes
      .flatMap(j => fromOption(j.hcursor.downField("episodes").focus))
      .flatMap(j => fromEither(j.as[Array[SERDE.Episode]]))
      .catchAll(th => ZIO.fail(new RuntimeException(th.toString)))

  val app: HttpApp[AppConfig & Client, Throwable] =
    Http
      .collectHttp[Request] { case Method.GET -> !! / "raw" / name =>
        Http.fromStream(ZStream.fromFile(Paths.get("./templates/ogrodje-white-logo.png").toFile))
      } ++ Http.collectZIO[Request] {
      case Method.GET -> !! / "episode" / code       =>
        getEpisode(code).flatMap { case (episode, episodeSummary) =>
          Render.render("episode.mustache", "episode" -> episode, "episodeSummary" -> episodeSummary)
        }
      case Method.GET -> !! / "episode-video" / code =>
        getEpisode(code).flatMap { case (episode, episodeSummary) =>
          Render.render("episode-video.mustache", "episode" -> episode, "episodeSummary" -> episodeSummary)
        }
      case Method.GET -> !! / "episodes"             =>
        getEpisodes.flatMap { episodes =>
          Render.render("episodes.mustache", "episodes" -> episodes)
        }
      case _                                         =>
        ZIO.succeed(Response.text("404"))
    }

  val config      = ServerConfig.default
    .port(7777)
    .leakDetection(LeakDetectionLevel.SIMPLE)
  val configLayer = ServerConfig.live(config)

  def run =
    (Server.install(app).flatMap { port =>
      Console.printLine(s"Started server on port: $port")
    } *> ZIO.never)
      .provide(Scope.default, Client.default, ZLayer.fromZIO(AppConfig.load), configLayer, Server.live)
    // Server.install(app).provide(ServerConfig.live(config), Server.live)
  // app.provide(Scope.default, Client.default, ZLayer.fromZIO(AppConfig.load))
