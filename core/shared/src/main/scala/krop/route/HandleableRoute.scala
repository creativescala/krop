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
import krop.KropRuntime
import krop.WithRuntime

/** Adds the handler API to an internal route.
  */
trait HandleableRoute[E <: Tuple, R] extends InternalRoute[E, R] {
  import HandleableRoute.{HandlerIOBuilder, HandlerPureBuilder}

  /** Handler incoming requests with the given function. */
  def handle(using ta: TupleApply[E, R]): HandlerPureBuilder[E, ta.Fun, R] =
    HandlerPureBuilder(this, ta)

  /** Handler incoming requests with the given function. */
  def handleIO(using ta: TupleApply[E, IO[R]]): HandlerIOBuilder[E, ta.Fun, R] =
    HandlerIOBuilder(this, ta)

  /** Pass the result of parsing the request directly the response with no
    * modification.
    */
  def passthrough(using pb: PassthroughBuilder[E, R]): Handler =
    Handler(this, runtime ?=> pb.build)
}
object HandleableRoute {

  /** This class exists to help type inference when constructing a Handler from
    * a Route.
    */
  final class HandlerPureBuilder[E <: Tuple, F, R](
      route: HandleableRoute[E, R],
      ta: TupleApply.Aux[E, F, R]
  ) {
    def apply(f: F): Handler = {
      val handle = ta.tuple(f)
      Handler(route, runtime ?=> i => IO.pure(handle(i)))
    }

    def apply(f: WithRuntime[F]): Handler = {
      val handle = (runtime: KropRuntime) ?=> ta.tuple(f)
      Handler(route, runtime ?=> i => IO.pure(handle(i)))
    }
  }

  /** This class exists to help type inference when constructing a Handler from
    * a Route.
    */
  final class HandlerIOBuilder[E <: Tuple, F, R](
      route: HandleableRoute[E, R],
      ta: TupleApply.Aux[E, F, IO[R]]
  ) {
    def apply(f: F): Handler =
      Handler(route, runtime ?=> ta.tuple(f))

    def apply(f: WithRuntime[F]): Handler =
      Handler(route, runtime ?=> ta.tuple(f))
  }
}
