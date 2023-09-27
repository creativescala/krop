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

package krop.route

import scala.util.Success
import scala.util.Try

/** A `Param` parses a path segment in a [[krop.route.Path]] into to a type `A`,
  * and converts a value of type `A` into a `String`.
  *
  * @param name
  *   The name used when printing this `Param`. Usually a short word in angle
  *   brackets, like "<int>" or "<string>".
  * @param parse
  *   The function to convert from a `String` to `A`, which can fail.
  * @param unparse
  *   The function to convert from `A` to a `String`.
  */
final case class Param[A](
    name: String,
    parse: String => Try[A],
    unparse: A => String
) {

  /** Create a `Path` with a more informative name. For example, you might use
    * this method to note that an int is in fact a user id.
    *
    * ```
    * Param.int.withName("<userId>")
    * ```
    */
  def withName(name: String): Param[A] =
    this.copy(name = name)
}
object Param {
  val int: Param[Int] = Param("<int>", str => Try(str.toInt), i => i.toString)
  val string: Param[String] = Param("<string>", str => Success(str), identity)
}
