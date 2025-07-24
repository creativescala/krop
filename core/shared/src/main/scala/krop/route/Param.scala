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

/** A [[package.Param]] is used to extract values from a URI's path or query
  * parameters.
  *
  * Params can also be inverted, going from a value of type `A` to a `String` or
  * sequence of `String`. This allows so-called reverse routing, constructing a
  * URI from the parameters.
  *
  * There are two types of `Param`:
  *
  * * those that handle a single value (`Param.One`); and
  *
  * * those that handle as many values as are available (`Param.All`).
  */
sealed abstract class Param[A] extends Product, Serializable {
  import Param.*

  /** Gets the name of this `Param`. By convention it describes the type within
    * angle brackets.
    */
  def name: String =
    this match {
      case All(codec) => codec.name
      case One(codec) => codec.name
    }

  /** Create a `Param` with a more informative name. For example, you might use
    * this method to note that an Int is in fact a user id.
    *
    * ```scala
    * Param.int.withName("<UserId>")
    * ```
    */
  def withName(name: String): Param[A] =
    this match {
      case All(codec) => All(codec.withName(name))
      case One(codec) => One(codec.withName(name))
    }
}
object Param {
  /* A `Param` that transforms a sequence of `String` to a value of type `A`.
   *
   * @param name
   *   The name used when printing this `Param`. Usually a short word in angle
   *   brackets, like "<Int>" or "<String>".
   * @param codec
   *   The [[SeqStringCodec]] that does encoding and decoding
   */
  final case class All[A](codec: SeqStringCodec[A]) extends Param[A] {
    export codec.{decode, encode}

    /** Construct a `Param.All[B]` from a `Param.All[A]` using functions to
      * convert from A to B and B to A.
      */
    def imap[B](f: A => B)(g: B => A): All[B] =
      All(codec.imap(f)(g))
  }

  /* A `Param` that matches a single parameter.
   *
   * @param name
   *   The name used when printing this `Param`. Usually a short word in angle
   *   brackets, like "<Int>" or "<String>".
   * @param codec
   *   The [[StringCodec]] that does encoding and decoding
   */
  final case class One[A](codec: StringCodec[A]) extends Param[A] {
    export codec.{decode, encode}

    /** Construct a `Param.One[B]` from a `Param.One[A]` using functions to
      * convert from A to B and B to A.
      */
    def imap[B](f: A => B)(g: B => A): One[B] =
      One(codec.imap(f)(g))
  }

  /** A `Param` that matches a single `Int` parameter */
  val int: Param.One[Int] =
    Param.One(StringCodec.int)

  /** A `Param` that matches a single `String` parameter */
  val string: Param.One[String] =
    Param.One(StringCodec.string)

  /** `Param` that simply accumulates all parameters as a `Seq[String]`.
    */
  val seq: Param.All[Seq[String]] =
    Param.All(SeqStringCodec.seqString)

  /** Constructs a [[Param]] that decodes input into a `String` by appending all
    * the input together with `separator` inbetween each element. Encodes data
    * by splitting on `separator`.
    *
    * For example,
    *
    * ```scala
    * val slash = Param.separatedString("/")
    * ```
    *
    * decodes `Seq("a", "b", "c")` to `"a/b/c"` and encodes `"a/b/c"` as
    * `Seq("a", "b", "c")`.
    */
  def separatedString(separator: String): Param.All[String] =
    Param.All(SeqStringCodec.separatedString(separator))

  def all[A](using codec: StringCodec[A]): Param.All[Seq[A]] =
    Param.All(SeqStringCodec.all(using codec))
}
