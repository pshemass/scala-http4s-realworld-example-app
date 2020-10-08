package com.hhandoko.realworld.http

import cats.Applicative
import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import com.hhandoko.realworld.core.Username
import com.hhandoko.realworld.http.auth.RequestAuthenticator
import com.hhandoko.realworld.repositories.UserRepo
import io.circe.{ Encoder, Json }
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{ AuthedRoutes, EntityEncoder, HttpRoutes }

object UserRoutes {

  def apply[F[_]: Sync: ContextShift](authenticated: RequestAuthenticator[F], userRepo: UserRepo[F]): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]; import dsl._

    authenticated {
      AuthedRoutes.of[Username, F] { case GET -> Root / "user" as username =>
        for {
          usrOpt <- userRepo.findUser(username)
          res    <- usrOpt.fold(NotFound()) { usr =>
                      Ok(UserResponse(usr.email, usr.token.value, usr.username.value, usr.bio, usr.image))
                    }
        } yield res
      }
    }
  }

  final case class UserResponse(
    email: String,
    token: String,
    username: String,
    bio: Option[String],
    image: Option[String]
  )

  object UserResponse {
    implicit val encoder: Encoder[UserResponse] = (r: UserResponse) =>
      Json.obj(
        "user" -> Json.obj(
          "email"    -> Json.fromString(r.email),
          "token"    -> Json.fromString(r.token),
          "username" -> Json.fromString(r.username),
          "bio"      -> r.bio.fold(Json.Null)(Json.fromString),
          "image"    -> r.image.fold(Json.Null)(Json.fromString)
        )
      )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, UserResponse] =
      jsonEncoderOf[F, UserResponse]
  }
}