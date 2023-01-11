package com.pinkstack.arttek

import com.pinkstack.arttek.SERDE.*
import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import zio.ZIO
import zio.ZIO.{fromEither, fromOption, succeed}
import zio.http.Client
import zio.stream.{ZPipeline, ZSink, ZStream}

object OgrodjeClient extends HYGraphClient:
  def episodes: QueryResponse[AppConfig & Client] =
    query("""
            |query LastEpisodes {
            |  episodes(orderBy: airedAt_DESC) {
            |    topics {
            |      name
            |    }
            |    name
            |    summary
            |    code
            |    airedAt
            |    host {
            |      fullName
            |      avatar {
            |        url
            |      }
            |    }
            |    cohosts {
            |      fullName
            |      avatar {
            |        url
            |      }
            |    }
            |    guests {
            |      fullName
            |      avatar {
            |        url
            |      }
            |    }
            |  }
            |}
            |""".stripMargin)

  def episode(code: String): QueryResponse[AppConfig & Client] =
    query(
      """
        |query GetEpisodeByCode($code: String!) {
        |  episode(where: {code: $code}) {
        |    topics {
        |      name
        |    }
        |    name
        |    summary
        |    code
        |    airedAt
        |    host {
        |      fullName
        |      avatar {
        |        url
        |      }
        |    }
        |    cohosts {
        |      fullName
        |      avatar {
        |        url
        |      }
        |    }
        |    guests {
        |      fullName
        |      avatar {
        |        url
        |      }
        |    }
        |  }
        |}
        |""".stripMargin,
      Map(
        "code" -> jsonFromString(code)
      )
    )

  def getEpisode(code: String): ZIO[AppConfig & Client, Throwable, (Episode, String)] =
    for
      episode        <- episode(code)
        .flatMap(j => fromOption(j.hcursor.downField("episode").focus))
        .flatMap(j => fromEither(j.as[SERDE.Episode]))
        .catchAll(th => ZIO.fail(new RuntimeException(th.toString)))
      episodeSummary <- Render.renderMarkdown(episode.summary)
    yield (episode, episodeSummary)

  def getEpisodes: ZIO[AppConfig & Client, Throwable, Array[Episode]] =
    episodes
      .flatMap(j => fromOption(j.hcursor.downField("episodes").focus))
      .flatMap(j => fromEither(j.as[Array[SERDE.Episode]]))
      .catchAll(th => ZIO.fail(new RuntimeException(th.toString)))
