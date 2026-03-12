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

import fs2.io.file.Path as Fs2Path

/** A [[Params]] is used to extract as many values as are available from a URI's
  * path segments.
  *
  * A `Params` can also be inverted, going from a value of type `A` to a
  * sequence of `String`. This allows so-called reverse routing, constructing a
  * URI from the parameters.
  *
  * To extract a single value from a path segment, use [[Param]].
  */
final case class Params[A](codec: SeqStringCodec[A]) {
  export codec.{decode, encode}

  /** Gets the name of this `Params`. By convention it describes the type within
    * angle brackets.
    */
  def name: String = codec.name

  /** Create a `Params` with a more informative name. */
  def withName(name: String): Params[A] = Params(codec.withName(name))

  /** Construct a `Params[B]` from a `Params[A]` using functions to convert from
    * A to B and B to A.
    */
  def imap[B](f: A => B)(g: B => A): Params[B] = Params(codec.imap(f)(g))
}
object Params {

  /** `Params` that simply accumulates all parameters as a `Seq[String]`. */
  val seq: Params[Seq[String]] =
    Params(SeqStringCodec.seqString)

  /** `Params` that converts path segments to a `fs2.io.file.Path`. */
  val fs2Path: Params[Fs2Path] =
    Params
      .separatedString("/")
      .imap(Fs2Path.apply)(_.toString)

  /** Constructs a [[Params]] that decodes input into a `String` by appending
    * all the input together with `separator` inbetween each element. Encodes
    * data by splitting on `separator`.
    *
    * For example,
    *
    * ```scala
    * val slash = Params.separatedString("/")
    * ```
    *
    * decodes `Seq("a", "b", "c")` to `"a/b/c"` and encodes `"a/b/c"` as
    * `Seq("a", "b", "c")`.
    */
  def separatedString(separator: String): Params[String] =
    Params(SeqStringCodec.separatedString(separator))

  def all[A](using codec: StringCodec[A]): Params[Seq[A]] =
    Params(SeqStringCodec.all(using codec))
}
