package com.pinkstack.arttek

import com.pinkstack.arttek.SERDE.*
import zio.*
import zio.ZIO.{attempt, fromEither, fromOption, logDebug, logInfo, succeed}
import Console.printLine
import zio.cli.*
import zio.cli.HelpDoc.Span.text
import zio.cli.figlet.FigFont
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.{Client, Server, ServerConfig}
import zio.logging.backend.SLF4J

import java.nio.file.{Files, Path, Paths}
import scala.util.Try

sealed trait Subcommand extends Product with Serializable
object Subcommand:
  final case class RenderYouTubeThumbnail(code: Code, outputFolder: Path)                        extends Subcommand
  final case class RenderPodcastThumbnail(code: Code, outputFolder: Path)                        extends Subcommand
  final case class RenderOverlay(code: Code, outputFolder: Path)                                 extends Subcommand
  final case class RenderPodcastThumbnails(outputFolder: Path, codes: Array[Code] = Array.empty) extends Subcommand
  final case class DevServer(port: Int)                                                          extends Subcommand

object ArttekApp extends ZIOCliDefault:
  import Subcommand.*

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val renderOverlay =
    Command("render-overlay", Args.text("code") ++ Args.path("output-folder"))
      .map(RenderOverlay.apply)

  val renderYouTubeThumbnail =
    Command("render-youtube-thumbnail", Args.text("code") ++ Args.path("output-folder"))
      .map(RenderYouTubeThumbnail.apply)

  val renderPodcastThumbnail =
    Command("render-podcast-thumbnail", Args.text("code") ++ Args.path("output-folder"))
      .map(RenderPodcastThumbnail.apply)

  val renderPodcastThumbnails =
    Command(
      "render-podcast-thumbnails",
      Options.text("codes").withDefault(""),
      Args.path("output-folder")
    ).map { case (rawCodes, path) =>
      RenderPodcastThumbnails.apply(path, rawCodes.split(",").filterNot(_.isEmpty))
    }

  val devServer = Command("dev-server", Options.text("port").withDefault("5555")).map { p =>
    DevServer.apply(
      Try(Integer.parseInt(p)).fold(_ => 5555, identity)
    )
  }

  val app: Command[Subcommand] =
    Command("arttek", Options.none, Args.none).subcommands(
      devServer,
      renderOverlay,
      renderYouTubeThumbnail,
      renderPodcastThumbnail,
      renderPodcastThumbnails
    )

  private def runWithServer[A](
    work: ZIO[AppConfig & Client, Throwable, A],
    portOpt: Option[Int] = None
  ): ZIO[Any, Throwable, Unit] =
    val app = for {
      serverFib <- Server.install(ServerApp.app).fork
      workFib   <- (work *> logInfo("Work done") *> serverFib.interrupt).fork
      _         <- serverFib.join
      _         <- workFib.join
    } yield ()

    AppConfig.load
      .map(c => c.copy(port = portOpt.fold(c.port)(identity)))
      .flatMap(config =>
        app
          .provide(
            Client.default,
            Scope.default,
            ZLayer.fromZIO(succeed(config)),
            ServerConfig.live(
              ServerConfig.default
                .port(config.port)
                .leakDetection(LeakDetectionLevel.DISABLED)
            ),
            Server.live
          )
      )

  val cliApp = CliApp.make(
    "arttek art builder",
    "0.0.1",
    text("Generates images and video for your podcast"),
    command = app
  ) {
    case DevServer(port)                      =>
      runWithServer(logInfo(s"Dev server booted on http://localhost:${port}") *> ZIO.unit.forever, Some(port))
    case RenderOverlay(code, path)            =>
      runWithServer {
        Actions.renderOverlay(code, path)
      }
    case RenderYouTubeThumbnail(code, path)   =>
      runWithServer {
        Actions.renderYouTubeThumbnail(code, path)
      }
    case RenderPodcastThumbnail(code, path)   =>
      runWithServer {
        Actions.renderPodcastThumbnail(code, path)
      }
    case RenderPodcastThumbnails(path, codes) =>
      runWithServer {
        Actions.renderPodcastThumbnails(path, codes)
      }
  }
