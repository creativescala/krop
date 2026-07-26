// A simple plugin that sets the directory layout for Twirl to the Krop
// standard. This removes the excessive indirection the Maven standard uses.
// Inspired by PlayLayoutPlugin.

import sbt.*
import sbt.Keys.*

import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

object KropTwirlLayout extends AutoPlugin {
  // Must be explicitly enabled
  override def trigger = noTrigger

  override def requires = SbtTwirl

  override def projectSettings = Seq(
    Compile / TwirlKeys.compileTemplates / sourceDirectories := Seq(
      (Compile / scalaSource).value
    ),
    Test / TwirlKeys.compileTemplates / sourceDirectories := Seq(
      (Test / scalaSource).value
    )
  )
}
