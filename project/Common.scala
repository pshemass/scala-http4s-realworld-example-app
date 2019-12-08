import java.io.Closeable
import scala.io.Source

import sbt.Keys._
import sbt._

object Common {

  // Dependency versions
  private val circeVersion      = "0.12.3"
  private val http4sVersion     = "0.21.0-M6"
  private val logbackVersion    = "1.2.3"
  private val oauthJwtVersion   = "3.8.2"
  private val pureConfigVersion = "0.12.1"
  private val specs2Version     = "4.8.1"

  // Compiler plugin dependency versions
  private val kindProjectorVersion    = "0.10.1"
  private val betterMonadicForVersion = "0.3.0"

  val settings = Seq(
    organization := "com.hhandoko",
    name := "realworld",
    version := using(Source.fromFile("VERSION.txt")) { _.mkString },
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "ch.qos.logback"        %  "logback-classic"        % logbackVersion,
      "com.auth0"             %  "java-jwt"               % oauthJwtVersion,
      "com.github.pureconfig" %% "pureconfig"             % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
      "io.circe"              %% "circe-generic"          % circeVersion,
      "org.http4s"            %% "http4s-blaze-server"    % http4sVersion,
      "org.http4s"            %% "http4s-circe"           % http4sVersion,
      "org.http4s"            %% "http4s-dsl"             % http4sVersion,
      "org.specs2"            %% "specs2-core"            % specs2Version % Test
    ),

    // Add syntax for type lambdas
    // See: https://github.com/non/kind-projector
    addCompilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorVersion),

    // Desugaring scala `for` without implicit `withFilter`s
    // See: https://github.com/oleg-py/better-monadic-for
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForVersion)
  )

  /**
   * Basic auto-closing implementation for closeable resource.
   * 
   * @param res Closeable resource.
   * @param fn Lambda function performing resource operations.
   * @tparam T Resource type parameters.
   * @tparam U Lambda function result type parameters.
   * @return Lambda function result.
   */
  private[this] def using[T <: Closeable, U](res: T)(fn: T => U): U =
    try { fn(res) } finally { res.close() }

}
