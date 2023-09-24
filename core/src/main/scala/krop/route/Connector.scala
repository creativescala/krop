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
  given emptyTupleApply[C]: TupleApply[EmptyTuple, () => C, C] with {
    def tuple(f: () => C): EmptyTuple => C = (_) => f()
  }

  given tuple1Apply[A, C]: TupleApply[Tuple1[A], A => C, C] with {
    def tuple(f: A => C): Tuple1[A] => C = (a) => f(a(0))
  }

  given tupleNApply[A, C]: TupleApply[A, A => C, C] with {
    def tuple(f: A => C): A => C = f
  }
}
