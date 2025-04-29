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

/** A SeqStringCodec encodes a value as a sequence of String, and decodes a
  * value from a sequence of String. SeqStringCodecs are used for handling:
  *
  *   - path segments;
  *   - query parameters; and
  *   - form submission.
  */
trait SeqStringCodec[A] {

  /** A short description of this codec. By convention the name of the type this
    * codec encodes enclosed within angle brackets, and using * (0 or more) or +
    * (1 or more) to indicate the expected number of repetitions.
    */
  def name: String
  def decode(value: Seq[String]): Either[DecodeFailure, A]
  def encode(value: A): Seq[String]

  /** Construct a `SeqStringCodec[B]` from a `SeqStringCodec[A]` using functions
    * to convert from A to B and B to A.
    *
    * You should probably call `withName` after `imap`, to give the
    * `SeqStringCodec` you just created a more appropriate name.
    */
  def imap[B](f: A => B)(g: B => A): SeqStringCodec[B] = {
    val self = this
    new SeqStringCodec[B] {
      val name: String = self.name

      def decode(value: Seq[String]): Either[DecodeFailure, B] =
        self.decode(value).map(f)

      def encode(value: B): Seq[String] = self.encode(g(value))
    }
  }

  /** Create a new [[SeqStringCodec]] that works exactly the same as this
    * [[SeqStringCodec]] except it has the given name.
    */
  def withName(newName: String): SeqStringCodec[A] = {
    val self = this
    new SeqStringCodec[A] {
      val name: String = newName

      def decode(value: Seq[String]): Either[DecodeFailure, A] =
        self.decode(value)

      def encode(value: A): Seq[String] = self.encode(value)
    }
  }
}
object SeqStringCodec {

  /** A [[SeqStringCodec]] that passes through its input unchanged. */
  given seqString: SeqStringCodec[Seq[String]] =
    new SeqStringCodec[Seq[String]] {
      val name: String = "<String>*"

      def decode(
          value: Seq[String]
      ): Either[DecodeFailure, Seq[String]] =
        Right(value)

      def encode(value: Seq[String]): Seq[String] = value
    }

  /** Constructs a [[SeqStringCodec]] from a given [[StringCodec]]. Decoding
    * fails if no values are available.
    */
  given fromStringCodec[A](using codec: StringCodec[A]): SeqStringCodec[A] =
    new SeqStringCodec[A] {
      val name: String = s"${codec.name}+"

      def decode(value: Seq[String]): Either[DecodeFailure, A] =
        value.headOption
          .map(codec.decode(_))
          .getOrElse(Left(DecodeFailure(value, codec.name)))

      def encode(value: A): Seq[String] =
        Seq(codec.encode(value))
    }

  /** Constructs a [[SeqStringCodec]] from a given [[StringCodec]]. Decoding
    * returns None if no values are available; otherwise the value is Some of
    * the decoded values.
    */
  given optional[A](using codec: StringCodec[A]): SeqStringCodec[Option[A]] =
    new SeqStringCodec[Option[A]] {
      val name: String = s"${codec.name}*"

      def decode(
          value: Seq[String]
      ): Either[DecodeFailure, Option[A]] =
        value.headOption
          .map(
            codec
              .decode(_)
              .map(_.some)
          )
          .getOrElse(Right(None))

      def encode(value: Option[A]): Seq[String] =
        value.map(codec.encode).toSeq
    }

  /** Constructs a [[SeqStringCodec]] that decodes input into a `String` by
    * appending all the input together with `separator` inbetween each element.
    * Encodes data by splitting on `separator`.
    *
    * For example,
    *
    * ```scala
    * val comma = SeqStringCodec.separatedString(",")
    * ```
    *
    * decodes `Seq("a", "b", "c")` to `"a,b,c"` and encodes `"a,b,c"` as
    * `Seq("a", "b", "c")`.
    */
  def separatedString(separator: String): SeqStringCodec[String] = {
    val quotedSeparator = Pattern.quote(separator)
    new SeqStringCodec[String] {
      val name: String = s"(<String>$separator)*"

      def decode(value: Seq[String]): Either[DecodeFailure, String] =
        value.mkString(separator).asRight

      def encode(value: String): Seq[String] =
        ArraySeq.unsafeWrapArray(value.split(quotedSeparator))
    }
  }

  /** Constructs a [[SeqStringCodec]] from a [[StringCodec]], decoding only the
    * first value in the input, if one exists, and encoding values as a single
    * element `Seq`.
    */
  def one[A](using codec: StringCodec[A]): SeqStringCodec[A] = {
    new SeqStringCodec[A] {
      val name: String = codec.name

      def decode(value: Seq[String]): Either[DecodeFailure, A] =
        value.headOption
          .map(codec.decode(_))
          .getOrElse(Left(DecodeFailure(value, codec.name)))

      def encode(value: A): Seq[String] =
        Seq(codec.encode(value))
    }
  }

  /** Constructs a [[SeqStringCodec]] from a [[StringCodec]], decoding all the
    * values in the input.
    */
  def all[A](using codec: StringCodec[A]): SeqStringCodec[Seq[A]] = {
    new SeqStringCodec[Seq[A]] {
      val name: String = "${codec.name}*"

      def decode(value: Seq[String]): Either[DecodeFailure, Seq[A]] =
        value.traverse(codec.decode)

      def encode(value: Seq[A]): Seq[String] =
        value.map(codec.encode)
    }
  }
}
