package com.hhandoko.realworld.repositories

import java.time.OffsetDateTime

import doobie._
import Fragments.whereAndOpt
import cats.Monad
import cats.effect.Sync
import com.hhandoko.realworld.core.{Author, Comment, Username}
import doobie.implicits._
import doobie.implicits.javatime._

trait CommentRepository[F[_]] {
  def find(slag: Option[String]): F[Vector[Comment]]
}

object CommentRepository {
  case class CommentRow(id: Int,
                        createdAt: OffsetDateTime,
                        updatedAt: OffsetDateTime,
                        body: String,
                        authorUserName: Username,
                        authorName: Username,
                        authorBio: Option[String],
                        authorImage: Option[String]
                       )
  object SQL {
    def find(slag: Option[String]) = {
      val select = fr"""select
           |c.id,
           |c.created_at,
           |c.modified_at,
           |c.body,
           |p.username,
           |p.bio,
           |p.image
           |from comment c join profile p on c.author_username=p.username join article a on a.id=c.article_id""".stripMargin

      val slagWhere = slag.map(s => fr"a.slag = $s")

      (select ++ whereAndOpt(slagWhere))
        .query[CommentRow]
        .map(row =>
          Comment(
            id = row.id,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            body = row.body,
            author = Author(
              username = row.authorName,
              bio = row.authorBio,
              image = row.authorImage,
              following = false
            )
          )
        )
    }
  }

  def apply[F[_]: Monad: Sync](xa: Transactor[F]) =
    new CommentRepository[F] {
      override def find(slag: Option[String]): F[Vector[Comment]] =
        SQL.find(slag)
          .to[Vector]
          .transact(xa)
    }
}
