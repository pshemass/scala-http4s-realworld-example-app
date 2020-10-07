package com.hhandoko.realworld.http

import java.time.format.DateTimeFormatter

import cats.Applicative
import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import com.hhandoko.realworld.repositories.CommentRepository
import com.hhandoko.realworld.http.auth.RequestAuthenticator
import com.hhandoko.realworld.core.{ Article, Comment, Tag, Username }
import com.hhandoko.realworld.repositories.{ ArticleRepository, CommentRepository, UserRepo }
import io.circe._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ AuthedRoutes, EntityEncoder, HttpRoutes, _ }

object ArticleRoutes {

  def apply[F[_]: ContextShift: Sync](
    articleRepository: ArticleRepository[F],
    commentRepository: CommentRepository[F],
    userRepo: UserRepo[F],
    auth: RequestAuthenticator[F]
  ): HttpRoutes[F] = {
    object dsl              extends Http4sDsl[F]; import dsl._
    object LimitQueryParam  extends QueryParamDecoderMatcher[Int]("limit")
    object OffsetQueryParam extends QueryParamDecoderMatcher[Int]("offset")
    import Article._
    auth {
      AuthedRoutes.of[Username, F] {
        case req @ POST -> Root / "api" / "articles" as username                                                  =>
          userRepo.findProfile(username).flatMap {
            case None       => BadRequest()
            case Some(user) =>
              for {
                request  <- req.req.as[CreateRequest]
                response <- articleRepository
                              .create(request.title, request.description, request.body, request.tagList, user)
                              .flatMap(Created(_))
              } yield response
          }
        case req @ POST -> Root / "api" / "articles" as username                                                  =>
          userRepo.findProfile(username).flatMap {
            case None       => BadRequest()
            case Some(user) =>
              for {
                request  <- req.req.as[CreateRequest]
                response <- articleRepository
                              .create(request.title, request.description, request.body, request.tagList, user)
                              .flatMap(Created(_))
              } yield response
          }
        case GET -> Root / "api" / "articles" / "feed" :? LimitQueryParam(limit) +& OffsetQueryParam(offset) as _ =>
          for {
            arts <- articleRepository.find(limit = Some(limit), offset = Some(offset))
            res  <- Ok(ArticlesResponse(arts))
          } yield res
        case GET -> Root / "api" / "articles" / slag / "comments" as _                                            =>
          for {
            arts <- commentRepository.find(slag = Some(slag))
            res  <- Ok(CommentsResponse(arts))
          } yield res
      }
    } <+>
      HttpRoutes.of[F] {
        case GET -> Root / "api" / "articles" :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
          for {
            arts <- articleRepository.find(limit = Some(limit), offset = Some(offset))
            res  <- Ok(ArticlesResponse(arts))
          } yield res
        case GET -> Root / "api" / "articles" / slag                                                =>
          articleRepository.get(slag).flatMap {
            case None          => HttpError.notFound(s"cannot find article with $slag slag")
            case Some(article) => Ok(article)
          }
      }
  }

  object Article {
    implicit val encoder: Encoder[Article] = (a: Article) =>
      Json.obj(
        "article" ->
          Json.obj(
            "slug"           -> Json.fromString(a.slug),
            "title"          -> Json.fromString(a.title),
            "description"    -> Json.fromString(a.description),
            "body"           -> Json.fromString(a.body),
            "tagList"        -> Json.fromValues(a.tagList.map(tag => Json.fromString(tag.value))),
            "createdAt"      -> Json.fromString(a.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
            "updatedAt"      -> Json.fromString(a.updatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
            "favorited"      -> Json.fromBoolean(a.favorited),
            "favoritesCount" -> Json.fromLong(a.favoritesCount),
            "author"         -> Json.obj(
              "username"  -> Json.fromString(a.author.username.value),
              "bio"       -> a.author.bio.fold(Json.Null)(Json.fromString),
              "image"     -> a.author.image.fold(Json.Null)(Json.fromString),
              "following" -> Json.fromBoolean(a.author.following)
            )
          )
      )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, Article] =
      jsonEncoderOf[F, Article]

  }

  final case class CreateRequest(
    title: String,
    description: String,
    body: String,
    tagList: Set[Tag]
  )

  object CreateRequest {

    implicit val decoder: Decoder[CreateRequest] = (r: HCursor) => {
      val article = r.downField("article")
      for {
        title       <- article.downField("title").as[String]
        description <- article.downField("description").as[String]
        body        <- article.downField("body").as[String]
      } yield CreateRequest(title, description, body, Set.empty)
    }

    implicit def entityDecoder[F[_]: Applicative: Sync]: EntityDecoder[F, CreateRequest] =
      jsonOf[F, CreateRequest]
  }

  final case class ArticlesResponse(articles: Vector[Article])

  object ArticlesResponse {
    implicit val encoder: Encoder[ArticlesResponse] = (r: ArticlesResponse) =>
      Json.obj(
        "articles" -> Json.fromValues(
          r.articles.map { a =>
            Json.obj(
              "slug"           -> Json.fromString(a.slug),
              "title"          -> Json.fromString(a.title),
              "description"    -> Json.fromString(a.description),
              "body"           -> Json.fromString(a.body),
              "tagList"        -> Json.fromValues(a.tagList.map(tag => Json.fromString(tag.value))),
              "createdAt"      -> Json.fromString(a.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
              "updatedAt"      -> Json.fromString(a.updatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
              "favorited"      -> Json.fromBoolean(a.favorited),
              "favoritesCount" -> Json.fromLong(a.favoritesCount),
              "author"         -> Json.obj(
                "username"  -> Json.fromString(a.author.username.value),
                "bio"       -> a.author.bio.fold(Json.Null)(Json.fromString),
                "image"     -> a.author.image.fold(Json.Null)(Json.fromString),
                "following" -> Json.fromBoolean(a.author.following)
              )
            )
          }
        )
      )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, ArticlesResponse] =
      jsonEncoderOf[F, ArticlesResponse]
  }

  case class CommentsResponse(comments: Vector[Comment])

  object CommentsResponse {
    implicit val encoder: Encoder[CommentsResponse] = (r: CommentsResponse) =>
      Json.obj(
        "comments" -> Json.fromValues(
          r.comments.map { a =>
            Json.obj(
              "id"        -> Json.fromInt(a.id),
              "body"      -> Json.fromString(a.body),
              "createdAt" -> Json.fromString(a.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
              "updatedAt" -> Json.fromString(a.updatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
              "author"    -> Json.obj(
                "username"  -> Json.fromString(a.author.username.value),
                "bio"       -> a.author.bio.fold(Json.Null)(Json.fromString),
                "image"     -> a.author.image.fold(Json.Null)(Json.fromString),
                "following" -> Json.fromBoolean(a.author.following)
              )
            )
          }
        )
      )

    implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, CommentsResponse] =
      jsonEncoderOf[F, CommentsResponse]
  }

}
