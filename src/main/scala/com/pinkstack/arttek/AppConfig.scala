package com.pinkstack.arttek

import zio.{System, Task}
import zio.ZIO.fromEither
import zio.http.URL

final case class HYGraph(token: String, endpoint: URL)
final case class AppConfig(hygraph: HYGraph)

object AppConfig:
  private def readEnv(variable: String): Task[String] =
    System.env(variable).map {
      case Some(value) => value
      case None        => throw new RuntimeException(s"Missing variable ${variable}")
    }

  def load: Task[AppConfig] =
    for
      token    <- readEnv("HYGRAPH_TOKEN")
      endpoint <- readEnv("HYGRAPH_ENDPOINT").flatMap(s => fromEither(URL.fromString(s)))
    yield AppConfig(HYGraph(token, endpoint))
