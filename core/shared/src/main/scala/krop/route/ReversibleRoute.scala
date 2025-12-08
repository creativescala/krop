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

/** The type of Routes that allow reverse routing. That is, constructing paths
  * that link to this route.
  */
trait ReversibleRoute[Path <: Tuple, Query <: Tuple] {
  def request: Request[?, Path, Query, ?]

  /** Overload of `pathTo` for the case where the path has no parameters.
    */
  def pathTo(using ev: EmptyTuple =:= Path): String =
    pathTo(ev(EmptyTuple))

  /** Overload of `pathTo` for the case where the path has a single parameter.
    */
  def pathTo[B](param: B)(using ev: Tuple1[B] =:= Path): String =
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
  def pathTo(params: Path): String =
    request.pathTo(params)

  /** Overload of `pathAndQueryTo` for the case where the path has no
    * parameters.
    */
  def pathAndQueryTo(queryParams: Query)(using
      ev: EmptyTuple =:= Path
  ): String =
    pathAndQueryTo(ev(EmptyTuple), queryParams)

  /** Overload of `pathAndQueryTo` for the case where the path has a single
    * parameter.
    */
  def pathAndQueryTo[B](pathParam: B, queryParams: Query)(using
      ev: Tuple1[B] =:= Path
  ): String =
    pathAndQueryTo(ev(Tuple1(pathParam)), queryParams)

  /** Overload of `pathAndQueryTo` for the case where the query has a single
    * parameter.
    */
  def pathAndQueryTo[B](pathParams: Path, queryParam: B)(using
      ev: Tuple1[B] =:= Query
  ): String =
    pathAndQueryTo(pathParams, ev(Tuple1(queryParam)))

  /** Overload of `pathAndQueryTo` for the case where the path and query have a
    * single parameter.
    */
  def pathAndQueryTo[B, C](pathParam: B, queryParam: C)(using
      evP: Tuple1[B] =:= Path,
      evQ: Tuple1[C] =:= Query
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
  def pathAndQueryTo(pathParams: Path, queryParams: Query): String =
    request.pathAndQueryTo(pathParams, queryParams)

}
