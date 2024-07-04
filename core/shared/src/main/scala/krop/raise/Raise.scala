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

package krop.raise

import scala.util.boundary
import scala.util.boundary.Label
import scala.util.boundary.break

/** Effect that can raise an error of type `E`. */
trait Raise[E] {

  /** Raise an error of type E. The error parameter is call-by-need as the
    * handler may decide to not evaluate it and instead replace it with another
    * value. For example, it could be replaced with None if the context is
    * expecting an Option.
    */
  def raise(error: => E): Nothing
}

object Raise {

  /** Construct a Raise[E] that produces a value in a context expecting a value
    * of type A.
    */
  def apply[E, A](f: E => A)(using Label[A]): Raise[E] =
    new Raise[E] {
      def raise(error: => E): Nothing =
        break(f(error))
    }

  /** Construct a Raise[E] that produces a value in a context expecting a value
    * of type A, where the Raise instance ignores the value of type E and simply
    * produces a constant value of type A.
    */
  def const[E, A](a: A)(using Label[A]): Raise[E] =
    new Raise[E] {
      def raise(error: => E): Nothing =
        break(a)
    }

  def raiseToEither[E, A](using label: Label[Either[E, A]]): Raise[E] =
    Raise(e => Left(e))

  def raiseToOption[E, A](using label: Label[Option[A]]): Raise[E] =
    const(None)

  inline def toEither[E, A](inline body: Raise[E] ?=> A): Either[E, A] =
    boundary {
      given Raise[E] = raiseToEither
      Right(body)
    }

  inline def toOption[E, A](inline body: Raise[E] ?=> A): Option[A] =
    boundary {
      given Raise[E] = raiseToOption
      Some(body)
    }
}
