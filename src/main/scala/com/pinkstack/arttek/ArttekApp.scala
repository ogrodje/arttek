package com.pinkstack.arttek

import zio.*
import Console.printLine
import zio.cli.*
import zio.cli.HelpDoc.Span.text
import zio.cli.figlet.FigFont

sealed trait Subcommand extends Product with Serializable
object Subcommand:
  final case class BuildOverlay(code: String, force: Boolean)  extends Subcommand
  final case class RenderEpisode(code: String, force: Boolean) extends Subcommand

object ArttekApp extends ZIOCliDefault:

  val buildOverlay  = Command("build-overlay").map { p =>
    Subcommand.BuildOverlay("code", false)
  }
  val renderEpisode = Command("render-episode").map { p =>
    Subcommand.RenderEpisode("code", false)
  }

  val app: Command[Subcommand] =
    Command("arttek", Options.none, Args.none).subcommands(buildOverlay, renderEpisode)

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
  }
