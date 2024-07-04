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
import cats.syntax.all.*
import krop.raise.Raise
import org.http4s.EntityDecoder
import org.http4s.Media
import org.http4s.Method
import org.http4s.Request as Http4sRequest

/** A [[krop.route.Request]] describes a pattern within a [[org.http4s.Request]]
  * that, if matched, will be routed to a handler. For example, it can look for
  * a particular HTTP method, say GET, and a particular pattern within a path,
  * such as "/user/create".
  *
  * The idiomatic way to create to a Request is starting with defining the HTTP
  * method and URI path, using the methods such as `get`, and `post` on the
  * companion object.
  *
  * @tparam P
  *   The type of values extracted from the URI path.
  * @tparam Q
  *   The type of values extracted from the URI query parameters.
  * @tparam H
  *   The type of values extracted from headers and other parts of the request.
  * @tparam E
  *   The type of value extracted from the entity.
  * @tparam O
  *   The type of values that construct the entity. Used when creating a request
  *   that calls the Route containing this Request.
  */
final case class Request[P <: Tuple, Q <: Tuple, H <: Tuple, E, O] private (
    method: Method,
    path: Path[P, Q],
    headers: RequestHeaders[H],
    entity: Entity[E, O]
) {
  import Request.NormalizedAppend

  //
  // Combinators ---------------------------------------------------------------
  //

  /** Create a [[scala.String]] path suitable for embedding in HTML that links
    * to the path described by this [[package.Request]]. Use this to create
    * hyperlinks or form actions that call a route, without needing to hardcode
    * the route in the HTML.
    *
    * This path will not include settings like the entity or headers that this
    * [[package.Request]] may require. It is assumed this will be handled
    * elsewhere.
    */
  def pathTo(params: P): String =
    path.pathTo(params)

  /** Produces a human-readable representation of this [[package.Request]]. The
    * toString method is used to output the usual programmatic representation.
    */
  def describe: String =
    s"${method.toString()} ${path.describe} ${entity.encoder.contentType.map(_.mediaType).getOrElse("")}"

  def withEntity[E2, O2](entity: Entity[E2, O2]): Request[P, Q, H, E2, O2] =
    Request(method, path, headers, entity)

  def withMethod(method: Method): Request[P, Q, H, E, O] =
    Request(method, path, headers, entity)

  def withPath[P2 <: Tuple, Q2 <: Tuple](
      path: Path[P2, Q2]
  ): Request[P2, Q2, H, E, O] =
    Request(method, path, headers, entity)

  //
  // Interpreters --------------------------------------------------------------
  //

  /** Extract the values that this Request matches from a
    * [[org.http4s.Request]], returning [[scala.None]] if the given request
    * doesn't match what this is looking for.
    */
  def extract[F[_, _]: Raise.Handler](
      request: Http4sRequest[IO]
  ): IO[F[ParseFailure, NormalizedAppend[Tuple.Concat[P, Q], E]]] = {
    given EntityDecoder[IO, E] = entity.decoder

    // F[Tuple.Concat[P, Q]]
    val fPQ =
      Raise.handle(
        if request.method != method
        then
          Raise.raise(
            ParseFailure(
              ParseStage.Method,
              s"The request's method is not the expected method",
              s"Expected the request's method to be ${method}, but it was ${request.method}."
            )
          )
        else path.parse(request.uri)
      )

    Raise.mapToIO(fPQ)(value =>
      request
        .as[E]
        .map(e =>
          (e match {
            case ()    => value
            case other => (value :* other)
          }).asInstanceOf[NormalizedAppend[Tuple.Concat[P, Q], E]]
        )
    )
  }
}
object Request {

  /** Tuple.Append that treats Unit as EmptyTuple */
  type NormalizedAppend[A <: Tuple, B] <: Tuple =
    B match {
      case Unit       => A
      case EmptyTuple => A
      case _          => Tuple.Append[A, B]
    }

  def delete[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    Request.method(Method.DELETE, path)

  def get[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    Request.method(Method.GET, path)

  def head[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    Request.method(Method.HEAD, path)

  def patch[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    Request.method(Method.PATCH, path)

  def post[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    Request.method(Method.POST, path)

  def put[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    Request.method(Method.PUT, path)

  def method[P <: Tuple, Q <: Tuple](
      method: Method,
      path: Path[P, Q]
  ): Request[P, Q, EmptyTuple, Unit, Unit] =
    new Request(method, path, RequestHeaders.empty, Entity.unit)

}
