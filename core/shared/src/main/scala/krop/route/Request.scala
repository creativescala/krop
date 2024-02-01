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
import org.http4s.EntityDecoder
import org.http4s.Media
import org.http4s.Method
import org.http4s.Request as Http4sRequest

/** A [[krop.route.Request]] describes a pattern within a [[org.http4s.Request]]
  * that will be routed to a handler. For example, it can look for a particular
  * HTTP method, say GET, and a particular pattern within a path, such as
  * "/user/create".
  *
  * The idiomatic way to create to a Request is starting with defining the HTTP
  * method and URI path, using the methods such as `get`, and `post` on the
  * companion object.
  *
  * @tparam P
  *   The type of values extracted from the URI path.
  * @tparam Q
  *   The type of values extracted from the URI query parameters.
  * @tparam E
  *   The type of values extracted from other parts of the request (e.g. headers
  *   or entity).
  * @tparam O
  *   The type of values that construct the entity. Used when creating a request
  *   that calls the Route containing this Request.
  */
final class Request[P <: Tuple, Q, E, O](
    val method: Method,
    val path: Path[P, Q],
    val entity: Entity[E, O]
) {
  import Request.NormalizedAppend

  /** Extract the values that this Request matches from a
    * [[org.http4s.Request]], returning [[scala.None]] if the given request
    * doesn't match what this is looking for.
    */
  def extract(
      request: Http4sRequest[IO]
  ): IO[Option[NormalizedAppend[NormalizedAppend[P, Q], E]]] = {
    given EntityDecoder[IO, E] = entity.decoder

    Option
      .when(request.method == method)(())
      .flatMap(_ => path.extract(request.uri)) match {
      case None => IO.pure(None)
      case Some(value) =>
        request
          .as[E]
          .map(e =>
            (e match {
              case ()    => Some(value)
              case other => Some(value :* other)
            }).asInstanceOf[Option[NormalizedAppend[NormalizedAppend[P, Q], E]]]
          )
    }
  }

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

  def withEntity[E2, O2](entity: Entity[E2, O2]): Request[P, Q, E2, O2] =
    new Request(method, path, entity)

  def withMethod(method: Method): Request[P, Q, E, O] =
    new Request(method, path, entity)

  def withPath[P2 <: Tuple, Q2](path: Path[P2, Q2]): Request[P2, Q2, E, O] =
    new Request(method, path, entity)

}
object Request {

  /** Tuple.Append that treats Unit as EmptyTuple */
  type NormalizedAppend[A <: Tuple, B] <: Tuple =
    B match {
      case Unit       => A
      case EmptyTuple => A
      case _          => Tuple.Append[A, B]
    }

  def delete[P <: Tuple, Q](path: Path[P, Q]): Request[P, Q, Unit, Unit] =
    Request.method(Method.DELETE, path)

  def get[P <: Tuple, Q](path: Path[P, Q]): Request[P, Q, Unit, Unit] =
    Request.method(Method.GET, path)

  def post[P <: Tuple, Q](path: Path[P, Q]): Request[P, Q, Unit, Unit] =
    Request.method(Method.POST, path)

  def put[P <: Tuple, Q](path: Path[P, Q]): Request[P, Q, Unit, Unit] =
    Request.method(Method.PUT, path)

  def method[P <: Tuple, Q](
      method: Method,
      path: Path[P, Q]
  ): Request[P, Q, Unit, Unit] =
    new Request(method, path, Entity.unit)

}
