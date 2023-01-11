package com.pinkstack.arttek

import zio.*
import Console.printLine
import zio.stream.{ZPipeline, ZSink, ZStream}
import ZIO.{attempt, fromEither, fromOption, succeed}
import com.pinkstack.arttek.SERDE.Episode
import zio.cli.*
import zio.cli.HelpDoc.Span.text
import zio.cli.figlet.FigFont
import zio.http.Client
import sys.process.*

sealed trait Subcommand extends Product with Serializable
object Subcommand:
  final case class BuildOverlay(code: String, force: Boolean)  extends Subcommand
  final case class RenderEpisode(code: String, force: Boolean) extends Subcommand
  case object GetAllAvatars                                    extends Subcommand

object ArttekApp extends ZIOCliDefault:
  private def download(url: String): Task[Unit] =
    for
      wgetCommand <- succeed(
        s"wget ${url} --no-clobber --no-verbose --adjust-extension --content-disposition -P tmp/"
      )
      wgetF       <- attempt(wgetCommand.!!).fork
      _           <- wgetF.join *> printLine(s"Downloaded ${url}.")
    yield ()

  val buildOverlay  = Command("build-overlay").map { p =>
    Subcommand.BuildOverlay("code", false)
  }
  val renderEpisode = Command("render-episode").map { p =>
    Subcommand.RenderEpisode("code", false)
  }
  val getAllAvatars = Command("get-all-avatars").map { _ =>
    Subcommand.GetAllAvatars
  }

  val app: Command[Subcommand] =
    Command("arttek", Options.none, Args.none).subcommands(buildOverlay, renderEpisode, getAllAvatars)

  val cliApp = CliApp.make(
    "arttek art builder",
    "0.0.1",
    text("Generates images and video for your podcast"),
    command = app
  ) {
    case Subcommand.BuildOverlay(code, force)  =>
      printLine("build overlay")
    case Subcommand.RenderEpisode(code, force) =>
      printLine("render episode")
    case Subcommand.GetAllAvatars              =>
      (for
        episodes <- OgrodjeClient.getEpisodes
        avatars  <- succeed(episodes.flatMap { episode =>
          Array(episode.host.avatar.url) ++ episode.cohosts.map(_.avatar.url) ++ episode.guests.map(_.avatar.url)
        }.toSet)
        _        <- ZStream
          .fromIterable(avatars)
          .mapZIOParUnordered(3)(download)
          .run(ZSink.drain)
      yield ())
        .provide(
          Scope.default,
          Client.default,
          ZLayer.fromZIO(AppConfig.load)
        )
  }
