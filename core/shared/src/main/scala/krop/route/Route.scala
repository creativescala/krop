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

import cats.data.Chain
import cats.effect.IO

/** A [[krop.Route]] accepts a request and may produce a response, but is not
  * required to produce a response. A Route is the basic unit for building a web
  * service. The majority of the service will consist of routes (and their
  * associated handlers), with a final catch-all to deal with any requests that
  * are not handled by other routes.
  */
final class Route(val routes: Chain[Route.Atomic[?, ?, ?]]) {

  /** Try this route. If it fails to match, try the other route. */
  def orElse(that: Route): Route =
    new Route(this.routes ++ that.routes)

}
object Route {

  /** The empty route, which doesn't match any request. */
  val empty: Route = new Route(Chain.empty[Atomic[?, ?, ?]])

  /** The smallest unit of route. A Route can combine several atomic routes. */
  final class Atomic[P <: Tuple, E <: Tuple, O](
      val request: Request[P, E],
      val response: Response[O],
      val handler: Tuple.Concat[P, E] => IO[O]
  ) {
    def toRoute: Route =
      new Route(Chain(this))
  }

  def apply[P <: Tuple, E <: Tuple, O](
      request: Request[P, E],
      response: Response[O]
  )(using
      ta: TupleApply[Tuple.Concat[P, E], O],
      taIO: TupleApply[Tuple.Concat[P, E], IO[O]]
  ): RouteBuilder[P, E, ta.Fun, taIO.Fun, O] =
    RouteBuilder(request, response, ta, taIO)

  final class RouteBuilder[P <: Tuple, E <: Tuple, F, FIO, O](
      request: Request[P, E],
      response: Response[O],
      ta: TupleApply.Aux[Tuple.Concat[P, E], F, O],
      taIO: TupleApply.Aux[Tuple.Concat[P, E], FIO, IO[O]]
  ) {
    def handle(f: F): Route =
      Atomic(request, response, i => IO.pure(ta.tuple(f)(i))).toRoute

    def handleIO[A](f: FIO): Route =
      Atomic(request, response, taIO.tuple(f)).toRoute

    def passthrough(using
        pb: PassthroughBuilder[Tuple.Concat[P, E], O]
    ): Route =
      Atomic(request, response, pb.build).toRoute
  }
}
