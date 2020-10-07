package com.hhandoko.realworld

import cats.effect.{ Blocker, ExitCode, IO, IOApp }

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .flatMap(Server.run[IO])
      .use(_ => IO.never.as(ExitCode.Success))

}
