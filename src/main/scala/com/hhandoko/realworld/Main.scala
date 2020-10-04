package com.hhandoko.realworld

import cats.effect.{ExitCode, IO, IOApp, Blocker}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].flatMap(Server.run[IO])
      .use(_ => IO.never.as(ExitCode.Success))

}
