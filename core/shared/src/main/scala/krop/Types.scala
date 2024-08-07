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

package krop

object Types {

  /** A variant of Tuple.Concat that considers the right-hand type (B) before
    * the left-hand type. This enables it to produce smaller types for the
    * common case of appending tuples from left-to-right.
    */
  type TupleConcat[A <: Tuple, B <: Tuple] <: Tuple =
    B match {
      case EmptyTuple => A
      case _ =>
        A match {
          case EmptyTuple => B
          case _          => Tuple.Concat[A, B]
        }
    }

  /** A variant of Tuple.Append that treats Unit as the empty tuple. */
  type TupleAppend[A <: Tuple, B] <: Tuple =
    B match {
      case Unit       => A
      case EmptyTuple => A
      case _          => Tuple.Append[A, B]
    }
}
