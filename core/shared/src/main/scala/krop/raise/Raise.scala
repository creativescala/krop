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

import cats.effect.IO

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

  /** A handler for Raise effects. */
  trait Handler[F[_, _]] {
    def apply[E, A](body: Raise[E] ?=> A): F[E, A]

    def succeed[E, A](success: A): F[E, A]

    /** Interoperate with IO */
    def mapToIO[E, A, B](value: F[E, A])(f: A => IO[B]): IO[F[E, B]]
  }

  def raise[E](error: => E): Raise[E] ?=> Nothing =
    raise ?=> raise.raise(error)

  /** Lift a successful value */
  def succeed[F[_, _], E, A](success: A)(using handler: Handler[F]): F[E, A] =
    handler.succeed(success)

  def mapToIO[F[_, _], E, A, B](result: F[E, A])(f: A => IO[B])(using
      handler: Handler[F]
  ): IO[F[E, B]] =
    handler.mapToIO(result)(f)

  def handle[F[_, _], E, A](body: Raise[E] ?=> A)(using
      handler: Handler[F]
  ): F[E, A] =
    handler.apply(body)

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

  val toEither: Handler[Either] =
    new Handler[Either] {
      def apply[E, A](body: (Raise[E]) ?=> A): Either[E, A] =
        boundary {
          given Raise[E] = raiseToEither
          Right(body)
        }

      def succeed[E, A](success: A): Either[E, A] =
        Right(success)

      def mapToIO[E, A, B](
          value: Either[E, A]
      )(f: A => IO[B]): IO[Either[E, B]] =
        value match {
          case Left(e)  => IO.pure(Left(e))
          case Right(a) => f(a).map(b => Right(b))
        }
    }

  type ToOption[E, A] = Option[A]
  val toOption: Handler[ToOption] =
    new Handler[ToOption] {
      def apply[E, A](body: (Raise[E]) ?=> A): ToOption[E, A] =
        boundary {
          given Raise[E] = raiseToOption
          succeed(body)
        }

      def succeed[E, A](success: A): ToOption[E, A] =
        Some(success)

      def mapToIO[E, A, B](value: ToOption[E, A])(
          f: A => IO[B]
      ): IO[ToOption[E, B]] =
        value match {
          case None    => IO.pure(None)
          case Some(a) => f(a).map(b => Some(b))
        }

    }

  type ToNull[E, A] = A | Null
  val toNull: Handler[ToNull] =
    new Handler[ToNull] {
      def apply[E, A](body: (Raise[E]) ?=> A): ToNull[E, A] =
        boundary {
          given Raise[E] = const(null)
          body
        }

      def succeed[E, A](success: A): ToNull[E, A] =
        success

      def mapToIO[E, A, B](
          value: ToNull[E, A]
      )(f: A => IO[B]): IO[ToNull[E, B]] =
        if (value == null) then IO.pure(null)
        else f(value.asInstanceOf[A])

    }
}
