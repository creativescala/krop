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

/** A `Param` parses path segments in a [[krop.route.Path]] into values of type
  * `A`, and converts values of type `A` into a path segments.
  */
enum Param[A] {
  /* A `Param` that matches a all parameters.
   *
   * @param name
   *   The name used when printing this `Param`. Usually a short word in angle
   *   brackets, like "<int>" or "<string>".
   * @param parse
   *   The function to convert from a `String` to `A`, which can fail.
   * @param unparse
   *   The function to convert from `A` to a `String`.
   */
  case All(
      name: String,
      parse: Vector[String] => Try[A],
      unparse: A => Vector[String]
  )
  /* A `Param` that matches a single parameter.
   *
   * @param name
   *   The name used when printing this `Param`. Usually a short word in angle
   *   brackets, like "<int>" or "<string>".
   * @param parse
   *   The function to convert from a `String` to `A`, which can fail.
   * @param unparse
   *   The function to convert from `A` to a `String`.
   */
  case One(
      name: String,
      parse: String => Try[A],
      unparse: A => String
  )

  /** Create a `Path` with a more informative name. For example, you might use
    * this method to note that an int is in fact a user id.
    *
    * ```
    * Param.int.withName("<userId>")
    * ```
    */
  def withName(name: String): Param[A] =
    this match {
      case All(_, p, u) => All(name, p, u)
      case One(_, p, u) => One(name, p, u)
    }
}
object Param {

  /** A `Param` that matches a single `Int` parameter */
  val int: Param[Int] =
    Param.One("<Int>", str => Try(str.toInt), i => i.toString)

  /** A `Param` that matches a single `String` parameter */
  val string: Param[String] =
    Param.One("<String>", str => Success(str), identity)

  /** `Param` that matches all parameters, accumulating them as a
    * `Vector[String`.
    */
  val vector: Param[Vector[String]] =
    Param.All("<String>", vector => Success(vector), identity)
}
