package com.pinkstack.arttek

import com.pinkstack.arttek.OgrodjeClient.{getEpisode, getEpisodes, EpisodeWithDetails}
import com.pinkstack.arttek.SERDE.*
import zio.*
import zio.ZIO.{attempt, fromEither, fromOption, logDebug, logInfo, succeed}
import zio.Console.printLine
import zio.cli.*
import zio.cli.HelpDoc.Span.text
import zio.cli.figlet.FigFont
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.{Client, Server, ServerConfig}
import zio.logging.backend.SLF4J
import zio.logging.{console, LogFormat}
import zio.process.Command
import zio.stream.{ZPipeline, ZSink, ZStream}

import java.nio.file.{Files, Path, Paths}
import scala.sys.process.*

object Actions:
  private type ActionOnPath = ZIO[AppConfig, Throwable, Path]

  private def download(url: String, output: Path): ActionOnPath =
    for
      _           <- logInfo(s"Download starting: $url")
      wgetCommand <- Command(s"wget", url, "--no-verbose", "-O", output.toAbsolutePath.toString).linesStream
        .foreach(p => Console.printLine("wget -> ", p))
        .fork
      out         <- wgetCommand.join *>
        logInfo(s"Download completed: ${url} to ${output.toAbsolutePath}") *> succeed(output)
    yield out

  private def htmlToImage(
    inputPath: Path,
    outputPath: Path,
    params: (String, String)*
  ): ActionOnPath =
    for
      params <- succeed(params.map {
        case (k, v) if v.nonEmpty => s"$k $v"
        case (k, _)               => s"$k"
      }.mkString(" ").strip)

      args = params.split(" ") ++ Seq(inputPath.toAbsolutePath.toString, outputPath.toAbsolutePath.toString)
      _          <- logInfo(s"Running command: wkhtmltoimage ${args.mkString(" ")}")
      commandFib <- Command("wkhtmltoimage", args: _*).linesStream
        .foreach(p => Console.printLine(p))
        .fork
      out <- commandFib.join *> logInfo(s"Image generated at ${outputPath.toAbsolutePath}") *> succeed(outputPath)
    yield out

  def renderOverlay(code: Code, outputFolder: Path): ActionOnPath =
    for {
      server        <- ZIO.service[AppConfig].map(_.server)
      thumbnailHTML <- download(
        s"$server/episode-video-2/$code",
        Paths.get(outputFolder.toString, s"overlay-${code}.html")
      )
      thumbnail     <- htmlToImage(
        thumbnailHTML,
        Paths.get(outputFolder.toString, s"overlay-${code}.png"),
        "--format"  -> "png",
        "--width"   -> "1220",
        "--crop-w"  -> "1220",
        "--quality" -> "100"
      )
      _             <- attempt(Files.delete(thumbnailHTML))
    } yield thumbnail

  def renderYouTubeThumbnail(code: Code, outputFolder: Path): ActionOnPath =
    for {
      server        <- ZIO.service[AppConfig].map(_.server)
      thumbnailHTML <- download(
        s"$server/episode-yt-avatar/$code",
        Paths.get(outputFolder.toString, s"youtube-thumbnail-${code}.html")
      )
      thumbnail     <- htmlToImage(
        thumbnailHTML,
        Paths.get(outputFolder.toString, s"youtube-thumbnail-${code}.png"),
        "--format"  -> "png",
        "--width"   -> "1280",
        "--crop-w"  -> "1280",
        "--height"  -> "720",
        "--crop-h"  -> "720",
        "--quality" -> "100"
      )

      _ <- attempt(Files.delete(thumbnailHTML))
    } yield thumbnail

  def renderPodcastThumbnail(code: Code, outputFolder: Path): ActionOnPath =
    for {
      server        <- ZIO.service[AppConfig].map(_.server)
      thumbnailHTML <- download(
        s"$server/podcast-thumbnail/$code",
        Paths.get(outputFolder.toString, s"podcast-thumbnail-${code}.html")
      )
      thumbnail     <- htmlToImage(
        thumbnailHTML,
        Paths.get(outputFolder.toString, s"podcast-thumbnail-${code}.png"),
        "--format"  -> "png",
        "--width"   -> "1000",
        "--crop-w"  -> "1000",
        "--height"  -> "1000",
        "--crop-h"  -> "1000",
        "--quality" -> "100"
      )
      _             <- attempt(Files.delete(thumbnailHTML))
    } yield thumbnail

  def renderPodcastThumbnails(
    outputFolder: Path,
    rawCodes: Array[Code]
  ): ZIO[AppConfig & Client, Throwable, Path] =
    def getCodes: ZIO[AppConfig & Client, Throwable, Array[Code]] =
      if rawCodes.isEmpty then OgrodjeClient.getEpisodes.map(_.map(_.code))
      else succeed(rawCodes)

    getCodes.flatMap(codes =>
      ZStream
        .fromIterable(codes)
        .tap(code => printLine(code))
        .mapZIO(code => renderPodcastThumbnail(code, outputFolder))
        .runDrain
    ) *> succeed(outputFolder)

  def renderYouTubeThumbnails(
    outputFolder: Path,
    rawCodes: Array[Code]
  ): ZIO[AppConfig & Client, Throwable, Path] =
    def getCodes: ZIO[AppConfig & Client, Throwable, Array[Code]] =
      if rawCodes.isEmpty then OgrodjeClient.getEpisodes.map(_.map(_.code))
      else succeed(rawCodes)

    getCodes.flatMap(codes =>
      ZStream
        .fromIterable(codes)
        .tap(code => printLine(code))
        .mapZIO(code => renderYouTubeThumbnail(code, outputFolder))
        .runDrain
    ) *> succeed(outputFolder)
