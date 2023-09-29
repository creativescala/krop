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
trait TupleApply[A, F, C] {
  def tuple(f: F): A => C
}
object TupleApply {
  given emptyTupleFunction0Apply[C]: TupleApply[EmptyTuple, () => C, C] with {
    def tuple(f: () => C): EmptyTuple => C = (_) => f()
  }

  given emptyTupleAnyFunction1Apply[C]: TupleApply[EmptyTuple, Any => C, C]
  with {
    def tuple(f: Any => C): EmptyTuple => C = x => f(x)
  }

  given emptyTupleUnitFunction1Apply[C]: TupleApply[EmptyTuple, Unit => C, C]
  with {
    def tuple(f: Unit => C): EmptyTuple => C = x => f(())
  }

  given tuple1Apply[A, C]: TupleApply[Tuple1[A], A => C, C] with {
    def tuple(f: A => C): Tuple1[A] => C = (a) => f(a(0))
  }

  given tuple2Apply[A, B, C]: TupleApply[
    Tuple2[A, B],
    (A, B) => C,
    C
  ] with {
    def tuple(
        f: (A, B) => C
    ): Tuple2[A, B] => C = f.tupled
  }

  given tuple3Apply[A, B, C, D]: TupleApply[
    Tuple3[A, B, C],
    (A, B, C) => D,
    D
  ] with {
    def tuple(
        f: (A, B, C) => D
    ): Tuple3[A, B, C] => D = f.tupled
  }

  given tuple4Apply[A, B, C, D, E]: TupleApply[
    Tuple4[A, B, C, D],
    (A, B, C, D) => E,
    E
  ] with {
    def tuple(
        f: (A, B, C, D) => E
    ): Tuple4[A, B, C, D] => E = f.tupled
  }

  given tuple5Apply[A, B, C, D, E, F]: TupleApply[
    Tuple5[A, B, C, D, E],
    (A, B, C, D, E) => F,
    F
  ] with {
    def tuple(
        f: (A, B, C, D, E) => F
    ): Tuple5[A, B, C, D, E] => F = f.tupled
  }

  given tuple6Apply[A, B, C, D, E, F, G]: TupleApply[
    Tuple6[A, B, C, D, E, F],
    (A, B, C, D, E, F) => G,
    G
  ] with {
    def tuple(
        f: (A, B, C, D, E, F) => G
    ): Tuple6[A, B, C, D, E, F] => G = f.tupled
  }

  given tuple7Apply[A, B, C, D, E, F, G, H]: TupleApply[
    Tuple7[A, B, C, D, E, F, G],
    (A, B, C, D, E, F, G) => H,
    H
  ] with {
    def tuple(
        f: (A, B, C, D, E, F, G) => H
    ): Tuple7[A, B, C, D, E, F, G] => H = f.tupled
  }

  given tuple8Apply[A, B, C, D, E, F, G, H, I]: TupleApply[
    Tuple8[A, B, C, D, E, F, G, H],
    (A, B, C, D, E, F, G, H) => I,
    I
  ] with {
    def tuple(
        f: (A, B, C, D, E, F, G, H) => I
    ): Tuple8[A, B, C, D, E, F, G, H] => I = f.tupled
  }

  given tuple9Apply[A, B, C, D, E, F, G, H, I, J]: TupleApply[
    Tuple9[A, B, C, D, E, F, G, H, I],
    (A, B, C, D, E, F, G, H, I) => J,
    J
  ] with {
    def tuple(
        f: (A, B, C, D, E, F, G, H, I) => J
    ): Tuple9[A, B, C, D, E, F, G, H, I] => J = f.tupled
  }

  given tuple10Apply[A, B, C, D, E, F, G, H, I, J, K]: TupleApply[
    Tuple10[A, B, C, D, E, F, G, H, I, J],
    (A, B, C, D, E, F, G, H, I, J) => K,
    K
  ] with {
    def tuple(
        f: (A, B, C, D, E, F, G, H, I, J) => K
    ): Tuple10[A, B, C, D, E, F, G, H, I, J] => K = f.tupled
  }

  given tupleNApply[A, C]: TupleApply[A, A => C, C] with {
    def tuple(f: A => C): A => C = f
  }
}
