package com.hhandoko.realworld.repositories

import java.time.OffsetDateTime

import com.hhandoko.realworld.core._
import doobie._
import Fragments.whereAndOpt
import cats.Monad
import cats.effect.Sync
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.postgres.implicits._

trait ArticleRepository[F[_]] {

  def create(title: String, description: String, body: String, tagList: Set[Tag], username: Profile): F[Article]

  def get(slug: String): F[Option[Article]]

  def find(limit: Option[Int] = None, offset: Option[Int] = None, tag: Option[Tag] = None): F[Vector[Article]]

  def allTags: F[Vector[Tag]]

}

object ArticleRepository {

  case class ArticleRow(
    slug: String,
    title: String,
    description: String,
    body: String,
    tags: Array[String],
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    authorUserName: Username,
    authorName: Username,
    authorBio: Option[String],
    authorImage: Option[String]
  )

  object SQL {

    def create(slag: String, title: String, description: String, body: String, tagList: Set[Tag], username: Username) =
      sql"""insert into article(slag, title, description, body, tags, author_username)
             values ($slag, $title, $description, $body, ${tagList.map(_.value).toArray[String]}, ${username})""".update

    def find(
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      author: Option[Username] = None,
      slag: Option[String] = None,
      tag: Option[Tag] = None
    ) = {
      val select      =
        fr"""select
             a.slag,
             a.title,
             a.description,
             a.body,
             a.tags,
             a.created_at,
             a.modified_at,
             a.author_username,
             p.username,
             p.bio,
             p.image
             from article a join profile p on a.author_username=p.username"""
      val authorWhere = author.map(s => fr"p.username = $s")
      val slugWhere   = slag.map(s => fr"a.slag = $s")
      val tagWhere    = tag.map(t => fr"tags @> ARRAY[${t.value}]::varchar[]")

      val limitSql  = limit.map(limit => fr"LIMIT $limit").getOrElse(Fragment.empty)
      val offsetSql = offset.map(offset => fr"OFFSET $offset").getOrElse(Fragment.empty)
      (select ++ whereAndOpt(authorWhere, slugWhere, tagWhere) ++ limitSql ++ offsetSql)
        .query[ArticleRow]
        .map(row =>
          Article(
            slug = row.slug,
            title = row.title,
            description = row.description,
            body = row.body,
            tagList = row.tags.toSet.map(Tag),
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

    def tags =
      sql"""select DISTINCT unnest(tags) from article""".query[Tag]
  }

  def apply[F[_]: Monad: Sync](xa: Transactor[F]): ArticleRepository[F] =
    new ArticleRepository[F] {
      override def find(limit: Option[Int], offset: Option[Int], tag: Option[Tag]): F[Vector[Article]] =
        SQL
          .find(limit = limit, offset = offset, tag = tag)
          .to[Vector]
          .transact(xa)

      override def create(
        title: String,
        description: String,
        body: String,
        tagList: Set[Tag],
        profile: Profile
      ): F[Article] = {
        val slag = title.toLowerCase.replace(' ', '-')
        val now  = OffsetDateTime.now()
        SQL
          .create(slag, title, description, body, tagList, profile.username)
          .run
          .map(_ =>
            Article(
              slug = slag,
              title = title,
              description = description,
              body = body,
              tagList = tagList,
              createdAt = now,
              updatedAt = now,
              favorited = false,
              favoritesCount = 0L,
              author = Author(
                username = profile.username,
                bio = profile.bio,
                image = profile.image,
                following = false
              )
            )
          )
          .transact(xa)
      }

      override def get(slag: String): F[Option[Article]] =
        SQL
          .find(
            limit = Some(1),
            offset = None,
            author = None,
            slag = Some(slag)
          )
          .option
          .transact(xa)

      override def allTags: F[Vector[Tag]] =
        SQL.tags
          .to[Vector]
          .transact(xa)
    }
}
