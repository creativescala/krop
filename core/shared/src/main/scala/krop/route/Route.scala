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
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import krop.Application
import krop.KropRuntime
import org.http4s.HttpRoutes

/** Type alias for a [[package.Route]] that has extracts no [[package.Entity]]
  * from the request.
  */
type PathRoute[P <: Tuple, R] = Route[P, EmptyTuple, EmptyTuple, Unit, Unit, R]

/** Type alias for a [[package.Route]] that has extracts no [[package.Path]] or
  * [[package.Entity]]] parameters from the request.
  */
type SimpleRoute[P, R] =
  Route[EmptyTuple, EmptyTuple, EmptyTuple, Unit, Unit, R]

/** Type alias for a [[package.Route]] that has extracts no [[package.Entity]]
  * from the request and extracts a single parameter from the [[package.Path]].
  */
type Path1Route[P, R] = PathRoute[Tuple1[P], R]

/** A [[krop.Route]] accepts a request and produces a response. A Route is the
  * basic unit for building a web service. The majority of the service will
  * consist of routes (and their associated handlers), with a final catch-all to
  * deal with any requests that are not handled by other routes.
  *
  * @tparam P
  *   The type of the parameters extracted from the [[package.Path]].
  * @tparam Q
  *   The type of the query parameters extracted from the [[package.Path]].
  * @tparam E
  *   The type of the [[package.Entity]] extracted from the request.
  * @tparam O
  *   The type of the parameters used to build the [[package.Response]].
  * @tparam R
  *   The type of the parameters used to build the [[package.Response]].
  */
final class Route[P <: Tuple, Q <: Tuple, H <: Tuple, E, O, R](
    val request: Request[P, Q, H, E, O],
    val response: Response[R],
    val handler: Request.NormalizedAppend[Tuple.Concat[P, Q], E] => IO[R]
) {

  /** Try this Route. If it fails to match, pass control to the given
    * [[krop.Application]].
    */
  def orElse(that: Application): Application =
    this.toRoutes.orElse(that)

  /** Try this Route. If it fails to match, pass control to the given
    * [[package.Route]].
    */
  def orElse(that: Route[?, ?, ?, ?, ?, ?]): Routes =
    this.orElse(that.toRoutes)

  /** Try this Route. If it fails to match, pass control to the
    * [[package.Routes]].
    */
  def orElse(that: Routes): Routes =
    Routes(this +: that.routes)

  /** Overload of `pathTo` for the case where the path has no parameters.
    */
  def pathTo(using ev: EmptyTuple =:= P): String =
    pathTo(ev(EmptyTuple))

  /** Overload of `pathTo` for the case where the path has a single parameter.
    */
  def pathTo[B](param: B)(using ev: Tuple1[B] =:= P): String =
    pathTo(ev(Tuple1(param)))

  /** Create a [[scala.String]] path suitable for embedding in HTML that links
    * to the path described by this [[package.Route]] with the given parameters.
    * Use this to create hyperlinks or form actions that call a route, without
    * needing to hardcode the route in the HTML.
    *
    * For example, with the Route
    *
    * ```scala
    * val route =
    *   Route(
    *     Request.get(Path / "user" / Param.id / "edit"),
    *     Request.ok(Entity.html)
    *   )
    * ```
    *
    * calling
    *
    * ```scala
    * route.pathTo(1234)
    * ```
    *
    * produces the `String` `"/user/1234/edit"`.
    *
    * This version of `pathTo` takes the parameters as a tuple. There are two
    * overloads that take unwrapped parameters for the case where there are no
    * or a single parameter.
    */
  def pathTo(params: P): String =
    request.pathTo(params)

  def toRoutes: Routes =
    Routes(Chain(this))

  def toHttpRoutes(using runtime: KropRuntime): IO[HttpRoutes[IO]] =
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
}
object Route {
  import Request.NormalizedAppend

  def apply[P <: Tuple, Q <: Tuple, H <: Tuple, E, O, R](
      request: Request[P, Q, H, E, O],
      response: Response[R]
  )(using
      ta: TupleApply[NormalizedAppend[Tuple.Concat[P, Q], E], R],
      taIO: TupleApply[NormalizedAppend[Tuple.Concat[P, Q], E], IO[R]]
  ): RouteBuilder[P, Q, H, E, O, ta.Fun, taIO.Fun, R] =
    RouteBuilder(request, response, ta, taIO)

  final class RouteBuilder[P <: Tuple, Q <: Tuple, H <: Tuple, E, O, F, FIO, R](
      request: Request[P, Q, H, E, O],
      response: Response[R],
      ta: TupleApply.Aux[NormalizedAppend[Tuple.Concat[P, Q], E], F, R],
      taIO: TupleApply.Aux[NormalizedAppend[Tuple.Concat[P, Q], E], FIO, IO[
        R
      ]]
  ) {
    def handle(f: F): Route[P, Q, H, E, O, R] =
      new Route(request, response, i => IO.pure(ta.tuple(f)(i)))

    def handleIO[A](f: FIO): Route[P, Q, H, E, O, R] =
      new Route(request, response, taIO.tuple(f))

    def passthrough(using
        pb: PassthroughBuilder[NormalizedAppend[Tuple.Concat[P, Q], E], R]
    ): Route[P, Q, H, E, O, R] =
      new Route(request, response, pb.build)
  }
}
