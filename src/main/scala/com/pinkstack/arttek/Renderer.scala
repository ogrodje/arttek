package com.pinkstack.arttek

import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import io.bit3.jsass.{CompilationException, Compiler as SASSCompiler, Options, Output, OutputStyle}
import zio.http.*
import zio.stream.{ZPipeline, ZStream}
import zio.*
import ZIO.{attempt, logDebug, logError, logInfo, succeed}
import zio.process.Command

import java.io.StringWriter
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*

object Renderer:
  def renderMustache[K <: String, V](templateName: String, data: (K, V)*): ZIO[AppConfig, Throwable, Response] =
    for
      path     <- succeed(Paths.get(s"./templates"))
      mustache <- attempt(new DefaultMustacheFactory(path.toFile).compile(templateName))
      sw       <- succeed(new StringWriter())
      _        <- attempt(
        mustache.execute(
          sw,
          data.toMap.asJava
        )
      ) *> succeed(sw.close())
      _        <- logInfo(s"Successfully rendered mustache: $templateName")
    yield Response.html(sw.toString)

  def renderMarkdown(raw: String): ZIO[AppConfig, Throwable, String] =
    for
      parser   <- succeed(Parser.builder().build())
      document <- succeed(parser.parse(raw))
      renderer <- succeed(HtmlRenderer.builder().build())
    yield renderer.render(document)

  @deprecated("Use new lib")
  private def oldRenderSASS(fileName: String): ZIO[AppConfig, Throwable, Response] =
    for
      fullPath <- succeed(Paths.get("sass", fileName + ".scss"))
      compiler <- succeed(SASSCompiler())
      options  <- succeed {
        val options = new Options
        options.setOutputStyle(OutputStyle.COMPRESSED)
        options
      }

      tempFile <- succeed(java.io.File.createTempFile("arttek", ".css"))
      css      <- attempt(
        compiler.compileFile(fullPath.toUri, tempFile.toURI, options).getCss
      ).ensuring(
        logDebug(s"Deleting temp file: ${tempFile.toPath.toAbsolutePath}") *>
          attempt(tempFile.delete()).orDie
      )
      _        <- logInfo(s"Successfully rendered SASS: $fullPath")
    yield Response(
      status = Status.Ok,
      body = Body.fromString(css),
      // headers = Headers(HeaderNames.contentType, HeaderValues.textCss)
    )

  private def renderSASS2(fileName: String): ZIO[AppConfig, Throwable, Response] =
    for
      inputPath  <- succeed(Paths.get("sass", fileName + ".scss"))
      outputFile <- succeed(java.io.File.createTempFile("arttek", ".css"))
      args = s"-t compressed ${inputPath.toAbsolutePath} ${outputFile.toPath.toAbsolutePath}".split(" ")
      _ <- Command("sassc", args: _*).string
      _ <- logInfo(s"[renderSASS2] Output: ${outputFile.toPath.toAbsolutePath}")
    yield Response(
      status = Status.Ok,
      body = Body.fromStream(ZStream.fromFile(outputFile)),
      // TODO: Fix this
      // headers = Headers(HeaderNames.contentType, HeaderValues.textCss)
    )

  def renderSASS(filename: String): ZIO[AppConfig, Throwable, Response] =
    renderSASS2(filename).catchAll { th =>
      logError(th.getMessage) *>
        ZIO.succeed(
          Response(
            status = Status.InternalServerError,
            body = Body.fromString(s"Failed with - ${th.getMessage}")
          )
        )
    }
