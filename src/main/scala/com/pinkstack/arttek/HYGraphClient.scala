package com.pinkstack.arttek

import io.circe.Json
import io.circe.Json.{fromFields as jsonFromFields, fromString as jsonFromString}
import io.netty.util.CharsetUtil
import zio.Console.printLine
import zio.*
import zio.ZIO.{fail, fromEither, fromOption, service, succeed}
import zio.http.{Body, Client, Headers, Method, Request, URL}

trait HYGraphClient:
  type QueryResponse[Deps] = ZIO[Deps, Throwable, Json]

  protected def query(
    rawQuery: String,
    variables: Map[String, Json] = Map.empty
  ): ZIO[Client & Scope with AppConfig, Throwable, Json] =
    for
      hygraphConfig <- service[AppConfig].map(_.hygraph)
      query         <- succeed(
        jsonFromFields(
          Seq("query" -> jsonFromString(rawQuery), "variables" -> jsonFromFields(variables))
        ).toString
      )
      // TODO: Headers are missing!
      request = Request.post(hygraphConfig.endpoint, Body.fromString(query, CharsetUtil.UTF_8))
      /*
      body          <- Client
        .request(
          hygraphConfig.endpoint.encode,
          Method.POST,
          Headers.apply("Authorization", s"Bearer ${hygraphConfig.token}"),
          Body.fromString(query, CharsetUtil.UTF_8)
        )
        .flatMap(_.body.asString)

       */
      body          <- Client.request(request).flatMap(_.body.asString)
      json          <- fromEither(io.circe.parser.parse(body))
      data          <- fromOption(json.hcursor.downField("data").focus)
        .orDieWith(_ => new RuntimeException("Missing \"data\" attribute in response!"))
    yield data
