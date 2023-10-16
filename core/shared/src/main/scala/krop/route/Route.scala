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
import krop.tool.NotFound
import org.http4s.HttpRoutes

/** A [[krop.Route]] accepts a request and may produce a response, but is not
  * required to produce a response. A Route is the basic unit for building a web
  * service. The majority of the service will consist of routes (and their
  * associated handlers), with a final catch-all to deal with any requests that
  * are not handled by other routes.
  *
  * @tparam P
  *   The type of the parameters to the [[package.Path]]
  * @tparam E
  *   The type of the [[package.Entity]]
  * @tparam O
  *   The type of the [[package.Response]]
  */
final class Route[P <: Tuple, E <: Tuple, R](
    val request: Request[P, E],
    val response: Response[R],
    val handler: Tuple.Concat[P, E] => IO[R]
) {

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
    *     Request.get(Path.root / "user" / Param.id / "edit"),
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

  def toHttpRoutes: IO[HttpRoutes[IO]] =
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
  def apply[P <: Tuple, E <: Tuple, R](
      request: Request[P, E],
      response: Response[R]
  )(using
      ta: TupleApply[Tuple.Concat[P, E], R],
      taIO: TupleApply[Tuple.Concat[P, E], IO[R]]
  ): RouteBuilder[P, E, ta.Fun, taIO.Fun, R] =
    RouteBuilder(request, response, ta, taIO)

  final class RouteBuilder[P <: Tuple, E <: Tuple, F, FIO, R](
      request: Request[P, E],
      response: Response[R],
      ta: TupleApply.Aux[Tuple.Concat[P, E], F, R],
      taIO: TupleApply.Aux[Tuple.Concat[P, E], FIO, IO[R]]
  ) {
    def handle(f: F): Route[P, E, R] =
      new Route(request, response, i => IO.pure(ta.tuple(f)(i)))

    def handleIO[A](f: FIO): Route[P, E, R] =
      new Route(request, response, taIO.tuple(f))

    def passthrough(using
        pb: PassthroughBuilder[Tuple.Concat[P, E], R]
    ): Route[P, E, R] =
      new Route(request, response, pb.build)
  }
}
