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

/** A [[Param]] is used to extract a single value from a URI's path or query
  * parameters.
  *
  * A `Param` can also be inverted, going from a value of type `A` to a
  * `String`. This allows so-called reverse routing, constructing a URI from the
  * parameters.
  *
  * To extract all remaining path segments as a sequence, use [[Params]].
  */
final case class Param[A](codec: StringCodec[A]) {
  export codec.{decode, encode}

  /** Gets the name of this `Param`. By convention it describes the type within
    * angle brackets.
    */
  def name: String = codec.name

  /** Create a `Param` with a more informative name. For example, you might use
    * this method to note that an Int is in fact a user id.
    *
    * ```scala
    * Param.int.withName("<UserId>")
    * ```
    */
  def withName(name: String): Param[A] = Param(codec.withName(name))

  /** Construct a `Param[B]` from a `Param[A]` using functions to convert from A
    * to B and B to A.
    */
  def imap[B](f: A => B)(g: B => A): Param[B] = Param(codec.imap(f)(g))
}
object Param {

  /** A `Param` that matches a single `Int` parameter */
  val int: Param[Int] =
    Param(StringCodec.int)

  /** A `Param` that matches a single `String` parameter */
  val string: Param[String] =
    Param(StringCodec.string)
}
