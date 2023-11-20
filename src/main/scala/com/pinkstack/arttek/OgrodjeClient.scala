package com.pinkstack.arttek

import com.pinkstack.arttek.SERDE.*
import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import zio.{Scope, ZIO}
import zio.ZIO.{attempt, fail, fromEither, fromOption, succeed}
import zio.http.Client
import zio.stream.{ZPipeline, ZSink, ZStream}

object OgrodjeClient extends HYGraphClient:
  def episodes: QueryResponse[Client & Scope with AppConfig] =
    query("""
            |query LastEpisodes {
            |  episodes(first: 10, orderBy: airedAt_DESC) {
            |    topics {
            |      name
            |    }
            |    name
            |    summary
            |    code
            |    airedAt
            |    backgroundImage {
            |      url
            |    }
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

  def episode(code: String): QueryResponse[Client & Scope with AppConfig] =
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
        |    backgroundImage {
        |      url
        |    }
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
        |    show {
        |     name
        |     color
        |    }
        |  }
        |}
        |""".stripMargin,
      Map(
        "code" -> jsonFromString(code)
      )
    )

  final case class HasPerson(person: PersonWithPicture, has: Boolean = true)
  object HasPerson:
    val empty: HasPerson = HasPerson(PersonWithPicture(SERDE.Person("none", Avatar.apply("none"))), false)

  case class EpisodeWithDetails(
    episode: Episode,
    summary: String,
    people: Array[HasPerson],
    backgroundImageURL: String,
    showName: String = "",
    showColor: String = ""
  )

  private val fetchImageID: String => String = _.split("/").last

  def getEpisode(code: String): ZIO[Client & Scope with AppConfig, Throwable, EpisodeWithDetails] =
    for
      episode        <- episode(code)
        .flatMap(j => fromOption(j.hcursor.downField("episode").focus))
        .flatMap(j => fromEither(j.as[SERDE.Episode]))
        .catchAll(th => fail(new RuntimeException(th.toString)))
      episodeSummary <- Renderer.renderMarkdown(episode.summary)
      guests         <- succeed((episode.guests ++ episode.cohosts).map(p => HasPerson(PersonWithPicture(p))))
      preGap         <- succeed(
        Map(0 -> 3, 1 -> 1, 2 -> 1, 3 -> 0)
          .getOrElse(guests.length, 0)
          // .getOrElse(guests.length, throw new RuntimeException("Only works for <= 4."))
      )
      postGap        <- succeed(
        Map(0 -> 0, 1 -> 1, 2 -> 0, 3 -> 0)
          .getOrElse(guests.length, 0)
          // .getOrElse(guests.length, throw new RuntimeException("Only works for <= 4."))
      )

      people <- succeed {
        (Array(HasPerson(PersonWithPicture(episode.host))) ++ Array.fill(preGap)(HasPerson.empty) ++ guests ++
          Array.fill(postGap)(HasPerson.empty)).map {
          case hp @ HasPerson(pp, true) =>
            hp.copy(person = pp.copy(picture = fetchImageID(pp.person.avatar.url)))
          case p                        => p
        }
      }
    yield EpisodeWithDetails(
      episode,
      episodeSummary,
      people,
      episode.backgroundImage.map(image => fetchImageID(image.url)).getOrElse(""),
      episode.show.map(_.name).getOrElse(""),
      episode.show.map(_.color).getOrElse("")
    )

  def getEpisodes: ZIO[Client & Scope with AppConfig, Throwable, Array[Episode]] =
    episodes
      .flatMap(j => fromOption(j.hcursor.downField("episodes").focus))
      .flatMap(j => fromEither(j.as[Array[SERDE.Episode]]))
      .catchAll(th => fail(new RuntimeException(th.toString)))
