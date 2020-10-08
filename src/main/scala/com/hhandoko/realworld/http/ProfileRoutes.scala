package com.hhandoko.realworld.http

import cats.Applicative
import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import com.hhandoko.realworld.core.Username
import com.hhandoko.realworld.repositories.UserRepo
import io.circe.{ Encoder, Json }
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityEncoder, HttpRoutes }

object ProfileRoutes {

  def apply[F[_]: ContextShift: Sync](userRepo: UserRepo[F]): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]; import dsl._

    HttpRoutes.of[F] { case GET -> Root / "profiles" / username =>
      for {
        prfOpt <- userRepo.findProfile(Username(username))
        res    <- prfOpt.fold(NotFound()) { prf =>
                    Ok(ProfileResponse(prf.username.value, prf.bio, prf.image, following = false))
                  }
      } yield res
    }
  }

  final case class ProfileResponse(username: String, bio: Option[String], image: Option[String], following: Boolean)

  object ProfileResponse {
    implicit val encoder: Encoder[ProfileResponse] = (r: ProfileResponse) =>
      Json.obj(
        "profile" -> Json.obj(
          "username"  -> Json.fromString(r.username),
          "bio"       -> r.bio.fold(Json.Null)(Json.fromString),
          "image"     -> r.image.fold(Json.Null)(Json.fromString),
          "following" -> Json.fromBoolean(r.following)
        )
      )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, ProfileResponse] =
      jsonEncoderOf[F, ProfileResponse]
  }

}