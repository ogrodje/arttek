package com.pinkstack.arttek
import com.pinkstack.arttek.OgrodjeClient.{getEpisode, getEpisodes}
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
import zio.http.model.Status.Ok
import io.circe.*
import io.circe.generic.semiauto.*
import java.io.IOException
import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*
import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}

object RendererApp extends ZIOAppDefault:
  import SERDE.*

  val app: HttpApp[AppConfig & Client, Throwable] =
    Http
      .collectHttp[Request] { case Method.GET -> !! / "raw" / name =>
        Http.fromStream(ZStream.fromFile(Paths.get("./templates/ogrodje-white-logo.png").toFile))
      } ++ Http.collectZIO[Request] {
      case Method.GET -> !! / "css" / fileName    =>
        Renderer.renderSASS(fileName)
          .catchAll(th => succeed(Response.text(th.getMessage)))
      case Method.GET -> !! / "episode" / code    =>
        getEpisode(code).flatMap { case (episode, episodeSummary, people) =>
          Renderer.renderMustache(
            "episode.mustache",
            "episode"        -> episode,
            "episodeSummary" -> episodeSummary,
            "people"         -> people
          )
        }
      case Method.GET -> !! / templateName / code =>
        getEpisode(code).flatMap { case (episode, episodeSummary, people) =>
          Renderer.renderMustache(
            s"${templateName}.mustache",
            "episode"        -> episode,
            "episodeSummary" -> episodeSummary,
            "people"         -> people
          )
        }
      case Method.GET -> !! / "episodes"          =>
        getEpisodes.flatMap { episodes =>
          Renderer.renderMustache("episodes.mustache", "episodes" -> episodes)
        }
      case _                                      =>
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
