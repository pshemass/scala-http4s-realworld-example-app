package com.hhandoko.realworld

import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.{Server => BlazeServer}
import pureconfig.module.catseffect.loadConfigF
import com.hhandoko.realworld.article.{ArticleRepository, ArticleRoutes}
import com.hhandoko.realworld.auth.{AuthRoutes, AuthService, RequestAuthenticator}
import com.hhandoko.realworld.config.{Config, DbConfig}
import com.hhandoko.realworld.profile.{ProfileRoutes, ProfileService}
import com.hhandoko.realworld.tag.{TagRoutes, TagService}
import com.hhandoko.realworld.user.{UserRepo, UserRoutes, UserService}

import scala.concurrent.ExecutionContext.global
import org.http4s.server.middleware.CORS
//import org.http4s.Response
//import org.http4s.Status
//import com.hhandoko.realworld.HttpError.ErrorEntity


object Server {

  def run[F[_]: ConcurrentEffect: ContextShift: Timer](bloker: Blocker): Resource[F, BlazeServer[F]] = {
    for {
      conf <- config[F](bloker)
      xa    <- transactor[F](conf.db)
      articlesRepo = ArticleRepository(xa)
      userRepo = UserRepo(xa)
    authenticator = new RequestAuthenticator[F]()
    authService = AuthService.impl[F]
    profileService = ProfileService.impl[F]
    tagService = TagService.impl[F]
    userService = UserService.impl[F]
    routes =
      ArticleRoutes[F](articlesRepo, userRepo, authenticator) <+>
      AuthRoutes[F](authService) <+>
      ProfileRoutes[F](profileService) <+>
      TagRoutes[F](tagService) <+>
      UserRoutes[F](authenticator, userService)


      rts   = loggedRoutes(conf, routes)
      svr  <- server[F](conf, rts)
    } yield svr
  }

  //private[this] def mapError[F[_]: ConcurrentEffect: Sync](response: Response[F]): Response[F] =
  //  response match {
  //    case Status.ClientError(_) => response.withEntity(ErrorEntity("error" :: Nil))
  //    case _ => response
  //  }

  private[this] def config[F[_]: Sync: ContextShift](blocker: Blocker): Resource[F, Config] = {
    import pureconfig.generic.auto._

    Resource.liftF(loadConfigF[F, Config](blocker))
  }

  private[this] def loggedRoutes[F[_]: ConcurrentEffect](conf: Config, routes: HttpRoutes[F]): HttpRoutes[F] =
    Logger.httpRoutes(conf.log.httpHeader, conf.log.httpBody) { routes }

  private[this] def server[F[_]: ConcurrentEffect: ContextShift: Timer](
    config: Config,
    routes: HttpRoutes[F]
  ): Resource[F, BlazeServer[F]] = {
    import org.http4s.implicits._

    BlazeServerBuilder[F](global)
      .bindHttp(config.server.port, config.server.host)
      .withHttpApp(CORS(routes.orNotFound))
      .resource
  }

  private[this] def transactor[F[_]: Async: ContextShift](config: DbConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool(config.pool)
      be <- Blocker[F]
      tx <-
        HikariTransactor.newHikariTransactor(
          config.driver,
          config.url,
          config.user,
          config.password,
          ce,
          be)
    } yield tx
}
