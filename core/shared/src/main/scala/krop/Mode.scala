/*
 * Copyright 2023 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package krop

/** Krop can run in one of two modes: development and production. In development
  * mode it shows output that is useful for debugging and otherwise inspecting
  * the running state. In production this output is hidden.
  *
  * The mode is set by the krop.mode JVM system property. If it has the value of
  * "development" (without the quotes; any capitalization is fine) then the mode
  * is development. Otherwise it is production.
  *
  * The mode is determined when Krop starts.
  */
enum Mode {
  case Production
  case Development

  def isProduction: Boolean =
    this match {
      case Production  => true
      case Development => false
    }

  def isDevelopment: Boolean =
    this match {
      case Production  => false
      case Development => true
    }
}
object Mode {

  /** The name of the system property used to set the Krop mode. */
  val modeProperty = "krop.mode"

  /** The mode in which Krop is running. */
  val mode: Mode = {
    val property = System.getProperty(modeProperty)
    val m =
      if (property == null) Mode.Production
      else if (property.toLowerCase() == "development") Mode.Development
      else Mode.Production

    Logger.logger.info(s"Krop starting in $m mode")
    m
  }
}
