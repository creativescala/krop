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

import cats.data.Chain
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import krop.route.PassthroughBuilder
import krop.route.Request
import krop.route.Response
import krop.route.TupleApply
import krop.tool.NotFound
import org.http4s.HttpRoutes
import org.http4s.{Request as Http4sRequest}
import org.http4s.{Response as Http4sResponse}

/** A [[krop.Route]] accepts a request and may produce a response, but is not
  * required to produce a response. A Route is the basic unit for building a web
  * service. The majority of the service will consist of routes (and their
  * associated handlers), with a final catch-all to deal with any requests that
  * are not handled by other routes.
  */
final case class Route(routes: Chain[Route.Atomic]) {

  /** Try this route. If it fails to match, try the other route. */
  def orElse(that: Route): Route =
    Route(this.routes ++ that.routes)

  /** Convert this route into an [[krop.Application]] that first tries this
    * route and, if the route fails to match, passes the request to the `app`
    * Application.
    */
  def otherwise(app: Application): Application =
    app.copy(route = this.orElse(app.route))

  /** Convert this [[krop.Route.Route]] into an [[krop.Application]] by
    * responding to all unmatched requests with a NotFound (404) response.
    */
  def otherwiseNotFound: Application =
    this.otherwise(NotFound.notFound)

  /** Convert to the representation used by http4s */
  def toHttpRoutes: IO[HttpRoutes[IO]] =
    routes.foldLeftM(HttpRoutes.empty[IO])((accum, atom) =>
      atom.toHttpRoutes.map(r => accum <+> r)
    )
}
object Route {

  /** The smallest unit of route. A Route can combine several atomic routes. */
  enum Atomic {
    case Krop[P <: Tuple, E <: Tuple, O](
        request: Request[P, E],
        response: Response[O],
        handler: Tuple.Concat[P, E] => IO[O]
    )
    case Http4s(description: String, routes: IO[HttpRoutes[IO]])

    def toRoute: Route =
      Route(Chain(this))

    def toHttpRoutes: IO[HttpRoutes[IO]] =
      this match {
        case Http4s(description, routes) => routes
        case Krop(request, response, handler) =>
          val jvmResponse = route.JvmResponse.fromResponse(response)
          IO.pure(
            Kleisli(req =>
              OptionT(
                request
                  .extract(req)
                  .flatMap(maybeIn =>
                    maybeIn.traverse(in =>
                      handler(in).flatMap(out => jvmResponse.respond(req, out))
                    )
                  )
              )
            )
          )
      }
  }

  def apply[P <: Tuple, E <: Tuple, O](
      request: Request[P, E],
      response: Response[O]
  )(using
      ta: TupleApply[Tuple.Concat[P, E], O],
      taIO: TupleApply[Tuple.Concat[P, E], IO[O]]
  ): RouteBuilder[P, E, ta.Fun, taIO.Fun, O] =
    RouteBuilder(request, response, ta, taIO)

  /** Lift an `IO[HttpRoutes[IO]` into a [[krop.Route]]. */
  def liftRoutesIO(description: String, routes: IO[HttpRoutes[IO]]): Route =
    Atomic.Http4s(description, routes).toRoute

  /** Lift an [[org.http4s.HttpRoutes]] into a [[krop.Route]]. */
  def liftRoutes(description: String, routes: HttpRoutes[IO]): Route =
    Route.liftRoutesIO(description, IO.pure(routes))

  /** Lift a partial function into a [[krop.Route]]. */
  def lift(
      description: String,
      f: PartialFunction[Http4sRequest[IO], IO[Http4sResponse[IO]]]
  ): Route =
    Route.liftRoutes(description, HttpRoutes.of(f))

  /** The empty route, which doesn't match any request. */
  val empty: Route = Route(Chain.empty)

  final case class RouteBuilder[P <: Tuple, E <: Tuple, F, FIO, O](
      request: Request[P, E],
      response: Response[O],
      ta: TupleApply.Aux[Tuple.Concat[P, E], F, O],
      taIO: TupleApply.Aux[Tuple.Concat[P, E], FIO, IO[O]]
  ) {
    def handle(f: F): Route =
      Route.Atomic.Krop(request, response, i => IO.pure(ta.tuple(f)(i))).toRoute

    def handleIO[A](f: FIO): Route =
      Route.Atomic.Krop(request, response, taIO.tuple(f)).toRoute

    def passthrough(using
        pb: PassthroughBuilder[Tuple.Concat[P, E], O]
    ): Route =
      Route.Atomic.Krop(request, response, pb.build).toRoute
  }
}
