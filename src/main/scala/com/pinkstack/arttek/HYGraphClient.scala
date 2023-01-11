package com.pinkstack.arttek

import io.circe.Json
import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import io.netty.util.CharsetUtil
import zio.Console.printLine
import zio.*
import zio.ZIO.{fail, fromEither, fromOption, service, succeed}
import zio.http.model.{Headers, Method}
import zio.http.{Body, Client, Request, URL}

trait HYGraphClient:
  type QueryResponse[Deps] = ZIO[Deps, Throwable, Json]

  protected def query(
    rawQuery: String,
    variables: Map[String, Json] = Map.empty
  ): QueryResponse[AppConfig & Client] =
    for
      hygraphConfig <- service[AppConfig].map(_.hygraph)
      query         <- succeed(
        jsonFromFields(
          Seq(
            "query"     -> jsonFromString(rawQuery),
            "variables" -> jsonFromFields(variables)
          )
        ).toString
      )
      body          <- Client
        .request(
          hygraphConfig.endpoint.encode,
          Method.POST,
          Headers.apply("Authorization", s"Bearer ${hygraphConfig.token}"),
          Body.fromString(query, CharsetUtil.UTF_8)
        )
        .flatMap(_.body.asString)
      json          <- fromEither(io.circe.parser.parse(body))
      data          <- fromOption(json.hcursor.downField("data").focus)
        .orDieWith(_ => new RuntimeException("Missing \"data\" attribute in response!"))
    yield data
