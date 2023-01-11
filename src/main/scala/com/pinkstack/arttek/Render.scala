package com.pinkstack.arttek

import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import zio.http.Response
import zio.{Task, ZIO}

import java.io.StringWriter
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*

object Render:
  def render[K, V](templateName: String, data: (K, V)*): Task[Response] =
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
