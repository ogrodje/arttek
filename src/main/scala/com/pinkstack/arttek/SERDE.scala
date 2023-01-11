package com.pinkstack.arttek

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object SERDE:
  final case class Avatar(url: String)
  implicit val avatarDecoder: Decoder[Avatar] = deriveDecoder[Avatar]
  implicit val avatarEncoder: Encoder[Avatar] = deriveEncoder[Avatar]

  final case class Person(fullName: String, avatar: Avatar)
  implicit val personDecoder: Decoder[Person] = deriveDecoder[Person]
  implicit val personEncoder: Encoder[Person] = deriveEncoder[Person]

  final case class Topic(name: String)
  implicit val topicDecoder: Decoder[Topic] = deriveDecoder[Topic]
  implicit val topicEncoder: Encoder[Topic] = deriveEncoder[Topic]

  final case class Episode(
    name: String,
    summary: String,
    topics: Array[Topic],
    code: String,
    airedAt: String,
    host: Person,
    cohosts: Array[Person],
    guests: Array[Person]
  )
  implicit val episodeDecoder: Decoder[Episode] = deriveDecoder[Episode]
  implicit val episodeEncoder: Encoder[Episode] = deriveEncoder[Episode]
