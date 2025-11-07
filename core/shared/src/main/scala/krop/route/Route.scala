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
// type PathRoute[P <: Tuple, Q <: Tuple, R] = Route[P, Q, P, P, R]

/** Type alias for a [[package.Route]] that has extracts no [[package.Path]] or
  * [[package.Entity]]] parameters from the request.
  */
// type SimpleRoute[R] = Route[EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, R]

/** Type alias for a [[package.Route]] that has extracts no [[package.Entity]]
  * from the request and extracts a single parameter from the [[package.Path]].
  */
// type Path1Route[P, R] = PathRoute[Tuple1[P], EmptyTuple, R]

/** A [[krop.Route]] describes an HTTP request and an HTTP response,
  * encapsulating the HTTP specific parts of an endpoint.
  *
  * @tparam C
  *   The type of the values used to construct a request.
  * @tparam Path
  *   The type of the parameters extracted from the [[package.Path]].
  * @tparam Query
  *   The type of the query parameters extracted from the [[package.Path]].
  * @tparam E
  *   The type of all the values extracted from the request.
  * @tparam R
  *   The type of the value used to build the [[package.Response]].
  * @tparam P
  *   The type of the value produced in the [[package.Response]].
  */
trait Route[C <: Tuple, Path <: Tuple, Query <: Tuple, E <: Tuple, R, P] {

  /** The [[krop.route.Request]] associated with this Route. */
  def request: Request[C, Path, Query, E]

  /** The [[krop.route.Response]] associated with this Route. */
  def response: Response[R, P]

}
object Route {

  /** Represents the common case of a Route as a simple container for a Request
    * and a Response.
    */
  private final class BasicRoute[
      C <: Tuple,
      Path <: Tuple,
      Query <: Tuple,
      E <: Tuple,
      R,
      P
  ](val request: Request[C, Path, Query, E], val response: Response[R, P])
      extends Route[C, Path, Query, E, R, P]

  /** Construct a [[krop.route.Route]] from a [[krop.route.Request]] and a
    * [[krop.route.Response]].
    */
  def apply[C <: Tuple, Path <: Tuple, Query <: Tuple, E <: Tuple, R, P](
      request: Request[C, Path, Query, E],
      response: Response[R, P]
  ): Route[C, Path, Query, E, R, P] =
    BasicRoute(request, response)
}
