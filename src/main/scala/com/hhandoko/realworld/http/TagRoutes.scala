package com.hhandoko.realworld.http

import cats.Applicative
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import com.hhandoko.realworld.core.Tag
import com.hhandoko.realworld.tag.TagService
import io.circe.{Encoder, Json}
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes}

object TagRoutes {

  def apply[F[_]: Sync: ContextShift](tagService: TagService[F]): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]; import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "api" / "tags" =>
        for {
          tags <- tagService.getAll
          res  <- Ok(AllTagsResponse(tags))
        } yield res
    }
  }

  final case class AllTagsResponse(tags: Vector[Tag])

  object AllTagsResponse {
    implicit val encoder: Encoder[AllTagsResponse] = (r: AllTagsResponse) => Json.obj(
      "tags" -> Json.fromValues(r.tags.map(_.value).map(Json.fromString))
    )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, AllTagsResponse] =
      jsonEncoderOf[F, AllTagsResponse]
  }

}
