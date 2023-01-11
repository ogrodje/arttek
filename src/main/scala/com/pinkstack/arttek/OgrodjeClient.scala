package com.pinkstack.arttek

import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import zio.http.Client

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
