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

/** This type class connects tuples of values and functions, specifically
  * allowing application of functions to tuples that don't exactly match their
  * type signature. For example, it allows a function of no arguments to be
  * applied to the empty tuple, and a function of a single argument to be
  * applied to a tuple of one value.
  */
trait TupleApply[I, O] {
  type Fun

  def tuple(f: Fun): I => O
}
object TupleApply {
  type Aux[I, F, O] = TupleApply[I, O] { type Fun = F }

  given emptyTupleFunction0Apply[C]: TupleApply[EmptyTuple, C] with {
    type Fun = () => C
    def tuple(f: () => C): EmptyTuple => C = (_) => f()
  }

  given tuple1Apply[A, C]: TupleApply[Tuple1[A], C] with {
    type Fun = A => C
    def tuple(f: A => C): Tuple1[A] => C = (a) => f(a(0))
  }

  given tuple2Apply[A, B, C]: TupleApply[
    Tuple2[A, B],
    C
  ] with {
    type Fun = (A, B) => C

    def tuple(
        f: (A, B) => C
    ): Tuple2[A, B] => C = f.tupled
  }

  given tuple3Apply[A, B, C, D]: TupleApply[
    Tuple3[A, B, C],
    D
  ] with {
    type Fun = (A, B, C) => D

    def tuple(
        f: (A, B, C) => D
    ): Tuple3[A, B, C] => D = f.tupled
  }

  given tuple4Apply[A, B, C, D, E]: TupleApply[
    Tuple4[A, B, C, D],
    E
  ] with {
    type Fun = (A, B, C, D) => E

    def tuple(
        f: (A, B, C, D) => E
    ): Tuple4[A, B, C, D] => E = f.tupled
  }

  given tuple5Apply[A, B, C, D, E, F]: TupleApply[
    Tuple5[A, B, C, D, E],
    F
  ] with {
    type Fun = (A, B, C, D, E) => F

    def tuple(
        f: (A, B, C, D, E) => F
    ): Tuple5[A, B, C, D, E] => F = f.tupled
  }

  given tuple6Apply[A, B, C, D, E, F, G]: TupleApply[
    Tuple6[A, B, C, D, E, F],
    G
  ] with {
    type Fun = (A, B, C, D, E, F) => G

    def tuple(
        f: (A, B, C, D, E, F) => G
    ): Tuple6[A, B, C, D, E, F] => G = f.tupled
  }

  given tuple7Apply[A, B, C, D, E, F, G, H]: TupleApply[
    Tuple7[A, B, C, D, E, F, G],
    H
  ] with {
    type Fun = (A, B, C, D, E, F, G) => H

    def tuple(
        f: (A, B, C, D, E, F, G) => H
    ): Tuple7[A, B, C, D, E, F, G] => H = f.tupled
  }

  given tuple8Apply[A, B, C, D, E, F, G, H, I]: TupleApply[
    Tuple8[A, B, C, D, E, F, G, H],
    I
  ] with {
    type Fun = (A, B, C, D, E, F, G, H) => I

    def tuple(
        f: (A, B, C, D, E, F, G, H) => I
    ): Tuple8[A, B, C, D, E, F, G, H] => I = f.tupled
  }

  given tuple9Apply[A, B, C, D, E, F, G, H, I, J]: TupleApply[
    Tuple9[A, B, C, D, E, F, G, H, I],
    J
  ] with {
    type Fun = (A, B, C, D, E, F, G, H, I) => J

    def tuple(
        f: (A, B, C, D, E, F, G, H, I) => J
    ): Tuple9[A, B, C, D, E, F, G, H, I] => J = f.tupled
  }

  given tuple10Apply[A, B, C, D, E, F, G, H, I, J, K]: TupleApply[
    Tuple10[A, B, C, D, E, F, G, H, I, J],
    K
  ] with {
    type Fun = (A, B, C, D, E, F, G, H, I, J) => K

    def tuple(
        f: (A, B, C, D, E, F, G, H, I, J) => K
    ): Tuple10[A, B, C, D, E, F, G, H, I, J] => K = f.tupled
  }
}
