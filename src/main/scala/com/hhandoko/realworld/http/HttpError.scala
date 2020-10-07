package com.hhandoko.realworld.http

import cats.Applicative
import cats.effect.{ ContextShift, Sync }
import io.circe.{ Encoder, Json }
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl

object HttpError {
  final case class ErrorEntity(body: List[String])
  object ErrorEntity {
    implicit val encoder: Encoder[ErrorEntity] = (err: ErrorEntity) =>
      Json.obj(
        "errors" -> Json.obj(
          "body" -> Json.fromValues(err.body.map(Json.fromString))
        )
      )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, ErrorEntity] =
      jsonEncoderOf[F, ErrorEntity]
  }
  def notFound[F[_]: Sync: ContextShift](msg: String) = {
    val dsl = Http4sDsl[F]
    import dsl._

    NotFound(ErrorEntity(List(msg)))
  }

}
