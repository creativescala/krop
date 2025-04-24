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

/** A StringCodec encodes a value as a String, and decodes a value from a
  * String. StringCodecs are used for handling:
  *
  *   - path segments;
  *   - query parameters; and
  *   - form submission.
  */
trait StringCodec[A] {

  /** A short description of this codec. By convention the name of the type this
    * codec encodes.
    */
  def name: String
  def decode(value: String): Either[StringDecodeFailure, A]
  def encode(value: A): String

  /** Construct a `StringCodec[B]` from a `StringCodec[A]` using functions to
    * convert from A to B and B to A.
    *
    * You should probably call `withName` after `imap`, to give the
    * `StringCodec` you just created a more appropriate name.
    */
  def imap[B](f: A => B)(g: B => A): StringCodec[B] = {
    val self = this
    new StringCodec[B] {
      val name: String = self.name

      def decode(value: String): Either[StringDecodeFailure, B] =
        self.decode(value).map(f)

      def encode(value: B): String = self.encode(g(value))
    }
  }

  /** Create a new [[StringCodec]] that works exactly the same as this
    * [[StringCodec]] except it has the given name.
    */
  def withName(newName: String): StringCodec[A] = {
    val self = this
    new StringCodec[A] {
      val name: String = newName

      def decode(value: String): Either[StringDecodeFailure, A] =
        self.decode(value)
      def encode(value: A): String = self.encode(value)
    }
  }
}
object StringCodec {
  given int: StringCodec[Int] =
    new StringCodec[Int] {
      val name: String = "Int"

      def decode(value: String): Either[StringDecodeFailure, Int] =
        value.toIntOption.toRight(StringDecodeFailure(value, name))

      def encode(value: Int): String = value.toString
    }

  given string: StringCodec[String] =
    new StringCodec[String] {
      val name: String = "String"

      def decode(value: String): Either[StringDecodeFailure, String] =
        Right(value)

      def encode(value: String): String = value
    }
}

/** Indicates that decoding a string failed.
  *
  * @param input:
  *   The string for which decoding was attempted
  * @param expected:
  *   A description of what was expected. By convention this is the type name.
  */
final case class StringDecodeFailure(input: String, expected: String)
