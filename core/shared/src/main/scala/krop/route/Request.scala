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
import org.http4s.{Request as Http4sRequest}

/** A [[krop.route.Request]] describes a pattern within a [[org.http4s.Request]]
  * that will be routed to a handler. For example, it can look for a particular
  * HTTP method, say GET, and a particular pattern within a path, such as
  * "/user/create".
  *
  * The idiomatic way to create to a Request is starting with defining the HTTP
  * method and URI path, using the methods such as `get`, and `post` on the
  * companion object.
  *
  * @tparam Path
  *   The type of values extracted from the URI path.
  * @tparam Extra
  *   The type of values extracted from other parts of the request (e.g. headers
  *   or entity).
  */
trait Request[Path <: Tuple, Extra <: Tuple] {

  /** Extract the values that this Request matches from a
    * [[org.http4s.Request]], returning [[scala.None]] if the given request
    * doesn't match what this is looking for.
    */
  def extract(request: Http4sRequest[IO]): IO[Option[Tuple.Concat[Path, Extra]]]

  /** Produces a human-readable representation of this [[package.Request]]. The
    * toString method is used to output the usual programmatic representation.
    */
  def describe: String

  /** Create a [[scala.String]] path suitable for embedding in HTML that links
    * to the path described by this [[package.Request]]. Use this to create
    * hyperlinks or form actions that call a route, without needing to hardcode
    * the route in the HTML.
    *
    * This path will not include settings like the entity or headers that this
    * [[package.Request]] may require. It is assumed this will be handled
    * elsewhere.
    */
  def pathTo(params: Path): String
}
object Request {
  def delete[P <: Tuple](path: Path[P]): PathRequest[P] =
    Request.method(Method.DELETE, path)

  def get[P <: Tuple](path: Path[P]): PathRequest[P] =
    Request.method(Method.GET, path)

  def post[P <: Tuple](path: Path[P]): PathRequest[P] =
    Request.method(Method.POST, path)

  def put[P <: Tuple](path: Path[P]): PathRequest[P] =
    Request.method(Method.PUT, path)

  def method[P <: Tuple](method: Method, path: Path[P]): PathRequest[P] =
    PathRequest(method, path)

  /** A [[package.Request]] that only specifies a method and a [[package.Path]].
    * The simplest possible [[package.Request]].
    */
  final case class PathRequest[P <: Tuple](
      method: Method,
      path: Path[P]
  ) extends Request[P, EmptyTuple] {
    def withEntity[E](entity: Entity[E]): PathEntityRequest[P, E] =
      PathEntityRequest(method, path, entity)

    def withMethod(method: Method): PathRequest[P] =
      this.copy(method = method)

    def withPath[P2 <: Tuple](path: Path[P2]): PathRequest[P2] =
      this.copy(path = path)

    def pathTo(params: P): String = path.pathTo(params)

    def extract(
        request: Http4sRequest[IO]
    ): IO[Option[Tuple.Concat[P, EmptyTuple]]] = {
      IO.pure(
        Option
          .when(request.method == method)(())
          .flatMap(_ =>
            path
              .extract(request.pathInfo)
              .map(_.asInstanceOf[Tuple.Concat[P, EmptyTuple]])
          )
      )
    }

    def describe: String =
      s"${method.toString()} ${path.describe}"
  }

  /** A [[package.Request]] that specifies a method, [[package.Path]], and an
    * [[package.Entity]].
    */
  final case class PathEntityRequest[P <: Tuple, E](
      method: Method,
      path: Path[P],
      entity: Entity[E]
  ) extends Request[P, Tuple1[E]] {

    def pathTo(params: P): String = path.pathTo(params)

    def extract(
        request: Http4sRequest[IO]
    ): IO[Option[Tuple.Concat[P, Tuple1[E]]]] = {
      given EntityDecoder[IO, E] = entity.decoder

      IO.pure(
        Option
          .when(request.method == method)(())
          .flatMap(_ => path.extract(request.pathInfo))
      ).flatMap(maybePath =>
        maybePath match {
          case None => IO.pure(None)
          case Some(value) =>
            request
              .as[E]
              .map(e => Some(value ++ Tuple1(e)))
        }
      )
    }

    def describe: String =
      s"${method.toString()} ${path.describe} ${entity.encoder.contentType.map(_.mediaType).getOrElse("")}"
  }
}
