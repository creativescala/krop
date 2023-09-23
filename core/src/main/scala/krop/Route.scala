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

import cats.data.Kleisli
import cats.data.NonEmptyChain
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import krop.route.Request
import krop.route.Response
import krop.tool.NotFound
import org.http4s.HttpRoutes
import org.http4s.{Request as Http4sRequest}
import org.http4s.{Response as Http4sResponse}

/** A [[krop.Route.Route]] is a function that accepts a request and may produce
  * a response.
  */
enum Route {
  case Http4s(description: String, routes: IO[HttpRoutes[IO]])
  case RequestResponse[I, O](
      request: Request[I],
      response: Response[O],
      handler: I => IO[O]
  )
  case Compound(routes: NonEmptyChain[Route])

  /** Try this route. If it fails to match, try the other route. */
  def orElse(that: Route): Route =
    this match {
      // Flatten the routes as we build them
      case Compound(routes1) =>
        that match {
          case Compound(routes2) => Compound(routes1 ++ routes2)
          case other             => Compound(routes1 :+ other)
        }

      case _ =>
        that match {
          case Compound(routes) => Compound(this +: routes)
          case other            => Compound(this +: NonEmptyChain.of(other))
        }

    }

  /** Convert this route into an [[krop.Application]] that first tries this
    * route and, if the route fails to match, passes the request to the `app`
    * Application.
    */
  def otherwise(app: Application): Application =
    Application.lift(req =>
      for {
        r <- this.toHttpRoutes
        a <- app.unwrap
        result <- r.run(req).getOrElseF(a(req))
      } yield result
    )

  /** Convert this [[krop.Route.Route]] into an [[krop.Application]] by
    * responding to all unmatched requests with a NotFound (404) response.
    */
  def otherwiseNotFound: Application =
    this.otherwise(NotFound.notFound)

  def toHttpRoutes: IO[HttpRoutes[IO]] =
    this match {
      case Http4s(description, routes) => routes
      case RequestResponse(request, response, handler) =>
        IO.pure(
          Kleisli(req =>
            OptionT(
              request
                .extract(req)
                .flatMap(maybeIn =>
                  maybeIn.traverse(in =>
                    handler(in).flatMap(out => response.respond(req, out))
                  )
                )
            )
          )
        )
      case Compound(routes) =>
        routes.reduceLeftM(route => route.toHttpRoutes)((accum, route) =>
          route.toHttpRoutes.map(r => accum.orElse(r))
        )
    }

}
object Route {

  def apply[I, O](
      request: Request[I],
      response: Response[O]
  ): RouteBuilder[I, O] =
    RouteBuilder(request, response)

  /** Lift an [[org.http4s.HttpRoutes]] into a [[krop.Route]]. */
  def liftRoutesIO(description: String, routes: IO[HttpRoutes[IO]]): Route =
    Route.Http4s(description, routes)

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
  val empty: Route =
    Route.liftRoutes("empty", HttpRoutes.empty[IO])

  final case class RouteBuilder[I, O](
      request: Request[I],
      response: Response[O]
  ) {
    def handle(f: I => O): Route =
      this.handleIO(i => IO.pure(f(i)))

    def handleIO(f: I => IO[O]): Route =
      Route.RequestResponse(request, response, f)

    def passthrough(using ev: I =:= O): Route =
      this.handleIO(i => IO.pure(i))
  }
}
