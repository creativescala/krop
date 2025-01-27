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

import cats.syntax.all.*

import java.util.regex.Pattern
import scala.collection.immutable.ArraySeq

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
  def name: String

  /** Gets a human-readable description of this `Param`. */
  def describe: String =
    this match {
      case All(name, _, _) => s"$name*"
      case One(name, _, _) => name
    }

  /** Create a `Path` with a more informative name. For example, you might use
    * this method to note that an Int is in fact a user id.
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
  /* A `Param` that transforms a sequence of `String` to a value of type `A`.
   *
   * @param name
   *   The name used when printing this `Param`. Usually a short word in angle
   *   brackets, like "<int>" or "<string>".
   * @param parse
   *   The function to convert from a `String` to `A`, which can fail.
   * @param unparse
   *   The function to convert from `A` to a `String`.
   */
  final case class All[A](
      name: String,
      parse: Seq[String] => Either[ParamParseFailure, A],
      unparse: A => Seq[String]
  ) extends Param[A] {

    /** Construct a `Param.All[B]` from a `Param.All[A]` using functions to
      * convert from A to B and B to A.
      */
    def imap[B](f: A => B)(g: B => A): All[B] =
      All(name, v => parse(v).map(f), b => unparse(g(b)))
  }

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
  final case class One[A](
      name: String,
      parse: String => Either[ParamParseFailure, A],
      unparse: A => String
  ) extends Param[A] {

    /** Construct a `Param.One[B]` from a `Param.One[A]` using functions to
      * convert from A to B and B to A.
      */
    def imap[B](f: A => B)(g: B => A): One[B] =
      One(name, parse(_).map(f), g.andThen(unparse))
  }

  /** A `Param` that matches a single `Int` parameter */
  val int: Param.One[Int] =
    Param.One(
      "<Int>",
      str => str.toIntOption.toRight(ParamParseFailure(str, "<Int>")),
      _.toString
    )

  /** A `Param` that matches a single `String` parameter */
  val string: Param.One[String] =
    Param.One("<String>", Right(_), identity)

  /** `Param` that simply accumulates all parameters as a `Seq[String]`.
    */
  val seq: Param.All[Seq[String]] =
    Param.All("<String>", Right(_), identity)

  /** `Param` that matches all parameters and converts them to a `String` by
    * adding `separator` between matched elements. The inverse splits on this
    * separator.
    */
  def mkString(separator: String): Param.All[String] = {
    val quotedSeparator = Pattern.quote(separator)
    Param.All(
      s"<String>${separator}",
      seq => Right(seq.mkString(separator)),
      string => ArraySeq.unsafeWrapArray(string.split(quotedSeparator))
    )
  }

  /** Lift a `Param.One` to a `Param.All`. */
  def lift[A](one: One[A]): Param.All[Seq[A]] =
    Param.All(
      one.name,
      _.traverse(one.parse),
      _.map(one.unparse)
    )
}
