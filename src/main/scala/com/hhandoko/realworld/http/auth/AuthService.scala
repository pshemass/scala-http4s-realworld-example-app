package com.hhandoko.realworld.http.auth

import cats.Applicative

import com.hhandoko.realworld.core.{ User, Username }

trait AuthService[F[_]] {
  type Email    = String
  type Password = String
  def verify(email: Email, password: Password): F[Either[String, User]]
}

object AuthService extends JwtSupport {

  implicit def apply[F[_]](implicit ev: AuthService[F]): AuthService[F] = ev

  def impl[F[_]: Applicative]: AuthService[F] =
    new AuthService[F] {
      import cats.implicits._

      def verify(email: Email, password: Password): F[Either[String, User]] = {
        // TODO: Consolidate with UserService
        email.split('@').toVector match {
          case localPart +: _ =>
            val username = Username(localPart)
            Either.right(User(username, bio = None, image = None, email, encodeToken(username)))

          case _ =>
            // TODO: Create User authentication error ADTs
            Either.left("Invalid email format")
        }
      }.pure[F]
    }

}
