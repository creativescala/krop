import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  // Library Versions
  val catsVersion = "2.10.0"
  val catsEffectVersion = "3.5.1"
  val fs2Version = "3.6.1"
  val http4sVersion = "1.0.0-M40"
  val endpoints4sVersion = "1.10.0"
  val endpoints4sOpenApiVersion = "4.4.0"
  val scalaJsDomVersion = "2.4.0"
  val scalaTagsVersion = "0.12.0"

  val munitVersion = "0.7.29"
  val munitCatsVersion = "2.0.0-M3"

  // Libraries
  val catsEffect =
    Def.setting("org.typelevel" %%% "cats-effect" % catsEffectVersion)
  val catsCore = Def.setting("org.typelevel" %%% "cats-core" % catsVersion)

  val fs2Core = Def.setting("co.fs2" %% "fs2-core" % fs2Version)

  val http4sClient =
    Def.setting("org.http4s" %% "http4s-ember-client" % http4sVersion)
  val http4sServer =
    Def.setting("org.http4s" %% "http4s-ember-server" % http4sVersion)
  val http4sDsl = Def.setting("org.http4s" %%% "http4s-dsl" % http4sVersion)
  val http4sCirce = Def.setting("org.http4s" %%% "http4s-circe" % http4sVersion)

  val scalaTags = Def.setting("com.lihaoyi" %%% "scalatags" % scalaTagsVersion)

  val munit = Def.setting("org.scalameta" %% "munit" % munitVersion % "test")
  val munitCats =
    Def.setting(
      "org.typelevel" %%% "munit-cats-effect" % munitCatsVersion % "test"
    )
}
