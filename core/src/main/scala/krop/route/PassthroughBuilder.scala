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

import cats.effect.IO

/** This type class constructs handler function instances for the `passthrough`
  * method on `RouteBuilder`, as these methods don't fit the shapes handled by
  * `TupleApply`.
  */
trait PassthroughBuilder[I, O] {
  def build: I => IO[O]
}
object PassthroughBuilder {
  given identityBuilder[A]: PassthroughBuilder[A, A] with {
    def build: A => IO[A] = a => IO.pure(a)
  }

  given tuple1Builder[A]: PassthroughBuilder[Tuple1[A], A] with {
    def build: Tuple1[A] => IO[A] = a => IO.pure(a(0))
  }

  given toUnitBuilder: PassthroughBuilder[EmptyTuple, Unit] with {
    def build: EmptyTuple => IO[Unit] = _ => IO.unit
  }
}
