package com.hhandoko.realworld.http.auth

import scala.util.Random

import org.specs2.Specification
import org.specs2.matcher.MatchResult

import com.hhandoko.realworld.core.Username

class JwtSupportSpec extends Specification { def is = s2"""

  JWT support trait
    should generate token with valid username   $tokenHasValidUsername
  """

  object JwtTokenTester extends JwtSupport

  private val username = (1 to 10).map(_ => Random.alphanumeric).mkString

  private[this] val tokenToTest: JwtToken =
    JwtTokenTester.encodeToken(Username(username))

  private[this] def tokenHasValidUsername: MatchResult[Option[Username]] =
    JwtTokenTester.decodeToken(tokenToTest) must beSome(Username(username))
}
