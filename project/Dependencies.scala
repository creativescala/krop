import sbt.*
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*

object Dependencies {
  // Library Versions
  val catsVersion = "2.10.0"
  val catsEffectVersion = "3.5.1"
  val fs2Version = "3.6.1"
  val http4sVersion = "1.0.0-M44"
  val scalaJsDomVersion = "2.4.0"
  val scalaTagsVersion = "0.13.1"
  val log4catsVersion = "2.7.0"
  val logbackVersion = "1.5.18"

  val sqliteVersion = "3.49.1.0"
  val magnumVersion = "1.3.1"

  val munitVersion = "0.7.29"
  val munitCatsVersion = "2.0.0"

  // Libraries
  val catsEffect =
    Def.setting("org.typelevel" %%% "cats-effect" % catsEffectVersion)
  val catsCore = Def.setting("org.typelevel" %%% "cats-core" % catsVersion)

  val fs2Core = Def.setting("co.fs2" %%% "fs2-core" % fs2Version)

  val log4cats =
    Def.setting("org.typelevel" %%% "log4cats-core" % log4catsVersion)
  val log4catsSlf4j =
    Def.setting("org.typelevel" %%% "log4cats-slf4j" % log4catsVersion)
  val logback =
    Def.setting("ch.qos.logback" % "logback-classic" % logbackVersion % Runtime)

  val http4sClient =
    Def.setting("org.http4s" %%% "http4s-ember-client" % http4sVersion)
  val http4sServer =
    Def.setting("org.http4s" %%% "http4s-ember-server" % http4sVersion)
  val http4sDsl = Def.setting("org.http4s" %%% "http4s-dsl" % http4sVersion)
  val http4sCirce = Def.setting("org.http4s" %%% "http4s-circe" % http4sVersion)

  val sqlite = Def.setting("org.xerial" % "sqlite-jdbc" % sqliteVersion)
  val magnum = Def.setting("com.augustnagro" %% "magnum" % magnumVersion)

  val scalaTags = Def.setting("com.lihaoyi" %%% "scalatags" % scalaTagsVersion)

  val munit = Def.setting("org.scalameta" %%% "munit" % munitVersion % "test")
  val munitCats =
    Def.setting(
      "org.typelevel" %%% "munit-cats-effect" % munitCatsVersion % "test"
    )
}
