Global / onChangedBuildSource := ReloadOnSourceChanges

organization := "org.creativescala"
organizationName := "Creative Scala"
startYear := Some(2023)
licenses := Seq(License.Apache2)
developers := List(
  Developer(
    "noelwelsh",
    "Noel Welsh",
    "noel@welsh.me",
    url("https://github.com/noelwelsh")
  )
)

lazy val scala3 = "3.6.4"

crossScalaVersions := List(scala3)
scalaVersion := scala3
semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision
scalacOptions += "-Wunused:imports"

commands += Command.command("build") { state =>
  "clean" ::
    "compile" ::
    "test" ::
    "scalafixAll" ::
    "scalafmtAll" ::
    "scalafmtSbt" ::
    "dependencyUpdates" ::
    "reload plugins; dependencyUpdates; reload return" ::
    state
}

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    Dependencies.munitCats.value,
    Dependencies.log4cats.value,
    Dependencies.http4sClient.value,
    Dependencies.http4sServer.value,
    Dependencies.http4sDsl.value,
    Dependencies.http4sCirce.value,
    Dependencies.scalaTags.value,
    Dependencies.twirl.value
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(coreJvm, coreJs, sqlite, asset, examples, unidocs)
  .settings(name := "krop")

lazy val coreJvm = project
  .in(file("core/jvm"))
  .settings(
    commonSettings,
    moduleName := "krop-core",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala",
    Test / unmanagedSourceDirectories += baseDirectory.value / ".." / "shared" / "src" / "test" / "scala",
    libraryDependencies ++= Seq(
      Dependencies.declineEffect.value,
      Dependencies.log4catsSlf4j.value
    )
  )

lazy val coreJs = project
  .in(file("core/js"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    moduleName := "krop-core",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala",
    Test / unmanagedSourceDirectories += baseDirectory.value / ".." / "shared" / "src" / "test" / "scala"
  )

lazy val sqlite = project
  .in(file("sqlite"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Dependencies.sqlite.value,
      Dependencies.magnum.value
    ),
    moduleName := "krop-sqlite"
  )

lazy val asset = project
  .in(file("asset"))
  .settings(
    commonSettings,
    moduleName := "krop-asset"
  )
  .dependsOn(coreJvm)

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    name := "krop-docs"
  )

lazy val examples = project
  .in(file("examples"))
  .settings(
    commonSettings,
    moduleName := "krop-examples",
    run / javaOptions += "-Dkrop.mode=development",
    run / fork := true,
    libraryDependencies += Dependencies.logback.value
  )
  .dependsOn(coreJvm)
