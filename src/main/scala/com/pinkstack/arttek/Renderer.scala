package com.pinkstack.arttek

import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import io.bit3.jsass.{CompilationException, Compiler as SASSCompiler, Options, Output, OutputStyle}
import zio.http.{Body, Client, Http, HttpApp, Request, Response, URL, *}
import zio.http.model.*
import zio.stream.{ZPipeline, ZStream}
import zio.{Task, ZIO}

import java.io.StringWriter
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*

object Renderer:
  def renderMustache[K, V](templateName: String, data: (K, V)*): Task[Response] =
    for
      path     <- ZIO.succeed(Paths.get(s"./templates"))
      mustache <- ZIO.succeed(new DefaultMustacheFactory(path.toFile).compile(templateName))
      sw       <- ZIO.succeed(new StringWriter())
      _        <- ZIO.succeed(
        mustache.execute(
          sw,
          data.toMap.asJava
        )
      ) *> ZIO.succeed(sw.close())
    yield Response.html(sw.toString)

  def renderMarkdown(raw: String): Task[String] =
    for
      parser   <- ZIO.succeed(Parser.builder().build())
      document <- ZIO.succeed(parser.parse(raw))
      renderer <- ZIO.succeed(HtmlRenderer.builder().build())
    yield renderer.render(document)

  def renderSASS(fileName: String): Task[Response] =
    for
      raw      <- ZStream
        .fromPath(Paths.get("sass", fileName + ".scss"))
        .via(ZPipeline.utfDecode >>> ZPipeline.splitLines)
        .runFold("")(_ + _)
      compiler <- ZIO.succeed(SASSCompiler())
      options  <- ZIO.succeed {
        val options = new Options
        options.setOutputStyle(OutputStyle.COMPRESSED)
        options
      }
      css      <- ZIO.attempt(
        compiler.compileString(raw, options).getCss
      )
    yield Response(
      status = Status.Ok,
      body = Body.fromString(css),
      headers = Headers(HeaderNames.contentType, HeaderValues.textCss)
    )
