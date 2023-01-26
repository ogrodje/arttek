package com.pinkstack.arttek

import zio.{System, Task}
import zio.ZIO.{attempt, fromEither, fromTry, succeed}
import zio.http.URL

import scala.util.Try

final case class HYGraph(token: String, endpoint: URL)
final case class AppConfig(hygraph: HYGraph, port: Int = 4444) {
  val server: String = s"http://localhost:$port"
}

object AppConfig:
  private def readEnv(variable: String): Task[String] =
    System.env(variable).map {
      case Some(value) => value
      case None        => throw new RuntimeException(s"Missing environment variable ${variable}")
    }

  def load: Task[AppConfig] =
    for
      token    <- readEnv("HYGRAPH_TOKEN")
      endpoint <- readEnv("HYGRAPH_ENDPOINT").flatMap(s => fromEither(URL.fromString(s)))
      port     <- System
        .envOrElse("PORT", "4444")
        .flatMap(raw => attempt(Integer.parseInt(raw)))
    yield AppConfig(HYGraph(token, endpoint), port)
