package com.hhandoko.realworld.repositories

import java.time.OffsetDateTime

import com.hhandoko.realworld.core._
import doobie._
import Fragments.whereAndOpt
import cats.Monad
import cats.effect.Sync
import doobie.implicits._
import doobie.implicits.javatime._

trait ArticleRepository[F[_]] {

  def create(title: String, description: String, body: String, tagList: Set[Tag], username: Profile): F[Article]

  def get(slug: String): F[Option[Article]]

  def find(limit: Option[Int] = None, offset: Option[Int] = None): F[Vector[Article]]

}

object ArticleRepository {

  case class ArticleRow(
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    authorUserName: Username,
    authorName: Username,
    authorBio: Option[String],
    authorImage: Option[String]
  )

  object SQL {
    def find(limit: Option[Int], offset: Option[Int], author: Option[Username], slag: Option[String]) = {
      val select      =
        fr"""select
             a.slag,
             a.title,
             a.description,
             a.body,
             a.created_at,
             a.modified_at,
             a.author_username,
             p.username,
             p.bio,
             p.image
             from article a join profile p on a.author_username=p.username"""
      val authorWhere = author.map(s => fr"p.username = $s")
      val slugWhere   = slag.map(s => fr"a.slag = $s")

      val limitSql  = limit.map(limit => fr"LIMIT $limit").getOrElse(Fragment.empty)
      val offsetSql = offset.map(offset => fr"OFFSET $offset").getOrElse(Fragment.empty)
      (select ++ whereAndOpt(authorWhere, slugWhere) ++ limitSql ++ offsetSql)
        .query[ArticleRow]
        .map(row =>
          Article(
            slug = row.slug,
            title = row.title,
            description = row.description,
            body = row.body,
            tagList = Set.empty,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            favorited = false,
            favoritesCount = 0L,
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

  def apply[F[_]: Monad: Sync](xa: Transactor[F]): ArticleRepository[F] =
    new ArticleRepository[F] {
      override def find(limit: Option[Int], offset: Option[Int]): F[Vector[Article]] =
        SQL
          .find(limit = limit, offset = offset, author = None, slag = None)
          .to[Vector]
          .transact(xa)

      override def create(
        title: String,
        description: String,
        body: String,
        tagList: Set[Tag],
        username: Profile
      ): F[Article] = {
        val slag = title.toLowerCase.replace(' ', '-')
        val now  = OffsetDateTime.now()
        sql"""insert into article(slag, title, description, body, author_username)
             values ($slag, $title, $description, $body, ${username.username})""".update.run
          .map(_ =>
            Article(
              slag,
              title,
              description,
              body,
              tagList,
              now,
              now,
              false,
              0L,
              Author(username.username, username.bio, username.image, false)
            )
          )
          .transact(xa)
      }

      override def get(slag: String): F[Option[Article]] =
        SQL
          .find(limit = Some(1), offset = None, author = None, slag = Some(slag))
          .option
          .transact(xa)
    }
}
