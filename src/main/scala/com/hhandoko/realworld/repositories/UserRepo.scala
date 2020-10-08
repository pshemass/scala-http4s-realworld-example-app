package com.hhandoko.realworld.repositories

import cats._
import cats.effect.Sync
import com.hhandoko.realworld.core.{ Profile, User, Username }
import doobie.implicits._
import doobie.util.transactor.Transactor

trait UserRepo[F[_]] {
  def findProfile(username: Username): F[Option[Profile]]
  def findUser(username: Username): F[Option[User]]
}

object UserRepo {

  object SQL {
    def findProfile(username: Username) =
      sql"""SELECT username
           |     , bio
           |     , image
           |  FROM profile WHERE lower(username) = lower(${username.value}) LIMIT 1""".stripMargin
        .query[Profile]

    def findUser(username: Username) =
      sql"""SELECT p.username
           |     , p.bio
           |     , p.image
           |     , p.email
           |     , 'aaaa'
           |  FROM profile p join auth a on p.username=a.profile_username WHERE lower(username) = lower(${username.value}) LIMIT 1""".stripMargin
        .query[User]

  }

  def apply[F[_]: Monad: Sync](xa: Transactor[F]): UserRepo[F] =
    new UserRepo[F] {
      def findProfile(username: Username): F[Option[Profile]] =
        SQL
          .findProfile(username)
          .option
          .transact(xa)

      override def findUser(username: Username): F[Option[User]] =
        SQL.findUser(username).option.transact(xa)
    }
}
