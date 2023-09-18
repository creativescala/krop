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
  val scalaJsDomVersion = "2.4.0"

  val munitVersion = "0.7.29"

  // Libraries
  val catsEffect =
    Def.setting("org.typelevel" %%% "cats-effect" % catsEffectVersion)
  val catsCore = Def.setting("org.typelevel" %%% "cats-core" % catsVersion)
  val fs2Core = Def.setting("co.fs2" %% "fs2-core" % fs2Version)
  val http4sClient =
    Def.setting("org.http4s" %% "http4s-ember-client" % http4sVersion)
  val http4sServer =
    Def.setting("org.http4s" %% "http4s-ember-server" % http4sVersion)
  val http4sDsl = Def.setting("org.http4s" %% "http4s-dsl" % http4sVersion)

  val munit = Def.setting("org.scalameta" %% "munit" % munitVersion % "test")
}
