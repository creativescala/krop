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

/** Type alias for a [[package.Route]] that has extracts no [[package.Entity]]
  * from the request.
  */
type PathRoute[P <: Tuple, Q <: Tuple, R] = Route[P, Q, P, P, R]

/** Type alias for a [[package.Route]] that has extracts no [[package.Path]] or
  * [[package.Entity]]] parameters from the request.
  */
type SimpleRoute[R] = Route[EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, R]

/** Type alias for a [[package.Route]] that has extracts no [[package.Entity]]
  * from the request and extracts a single parameter from the [[package.Path]].
  */
type Path1Route[P, R] = PathRoute[Tuple1[P], EmptyTuple, R]

/** A [[krop.Route]] accepts a request and produces a response. A Route is the
  * basic unit for building a web service. The majority of the service will
  * consist of routes (and their associated handlers), with a final catch-all to
  * deal with any requests that are not handled by other routes.
  *
  * @tparam P
  *   The type of the parameters extracted from the [[package.Path]].
  * @tparam Q
  *   The type of the query parameters extracted from the [[package.Path]].
  * @tparam I
  *   The type of all the values extracted from the request.
  * @tparam O
  *   The type of the values used to construct a request.
  * @tparam R
  *   The type of the value used to build the [[package.Response]].
  */
final class Route[P <: Tuple, Q <: Tuple, I <: Tuple, O <: Tuple, R](
    val request: Request[P, Q, I, O],
    val response: Response[R]
) {

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

  /** Overload of `pathAndQueryTo` for the case where the path has no
    * parameters.
    */
  def pathAndQueryTo(queryParams: Q)(using ev: EmptyTuple =:= P): String =
    pathAndQueryTo(ev(EmptyTuple), queryParams)

  /** Overload of `pathAndQueryTo` for the case where the path has a single
    * parameter.
    */
  def pathAndQueryTo[B](pathParam: B, queryParams: Q)(using
      ev: Tuple1[B] =:= P
  ): String =
    pathAndQueryTo(ev(Tuple1(pathParam)), queryParams)

  /** Overload of `pathAndQueryTo` for the case where the query has a single
    * parameter.
    */
  def pathAndQueryTo[B](pathParams: P, queryParam: B)(using
      ev: Tuple1[B] =:= Q
  ): String =
    pathAndQueryTo(pathParams, ev(Tuple1(queryParam)))

  /** Overload of `pathAndQueryTo` for the case where the path and query have a
    * single parameter.
    */
  def pathAndQueryTo[B, C](pathParam: B, queryParam: C)(using
      evP: Tuple1[B] =:= P,
      evQ: Tuple1[C] =:= Q
  ): String =
    pathAndQueryTo(evP(Tuple1(pathParam)), evQ(Tuple1(queryParam)))

  /** Create a [[scala.String]] path suitable for embedding in HTML that links
    * to the path described by this [[package.Request]] and also includes query
    * parameters. Use this to create hyperlinks or form actions that call a
    * route, without needing to hardcode the route in the HTML.
    *
    * This path will not include settings like the entity or headers that this
    * [[package.Request]] may require. It is assumed this will be handled
    * elsewhere.
    */
  def pathAndQueryTo(pathParams: P, queryParams: Q): String =
    request.pathAndQueryTo(pathParams, queryParams)

  /** Handler incoming requests with the given function. */
  def handle(using ta: TupleApply[I, R]): HandlerPureBuilder[I, ta.Fun, R] =
    HandlerPureBuilder(this, ta)

  /** Handler incoming requests with the given function. */
  def handleIO(using ta: TupleApply[I, IO[R]]): HandlerIOBuilder[I, ta.Fun, R] =
    HandlerIOBuilder(this, ta)

  /** Pass the result of parsing the request directly the response with no
    * modification.
    */
  def passthrough(using pb: PassthroughBuilder[I, R]): Handler[I, R] =
    Handler(this, pb.build)
}

/** This class exists to make type inference work better when constructing a
  * Handler from a Route.
  */
final class HandlerPureBuilder[I <: Tuple, F, R](
    route: Route[?, ?, I, ?, R],
    ta: TupleApply.Aux[I, F, R]
) {
  def apply(f: F): Handler[I, R] = Handler(route, i => IO.pure(ta.tuple(f)(i)))
}

/** This class exists to make type inference work better when constructing a
  * Handler from a Route.
  */
final class HandlerIOBuilder[I <: Tuple, F, R](
    route: Route[?, ?, I, ?, R],
    ta: TupleApply.Aux[I, F, IO[R]]
) {
  def apply(f: F): Handler[I, R] = Handler(route, ta.tuple(f))
}
