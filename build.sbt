enablePlugins(FlywayPlugin)

lazy val realworld =
  (project in file("."))
    .settings(Common.settings: _*)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
