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

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import org.http4s.DecodeResult
import org.http4s.EntityDecoder
import org.http4s.Media
import org.http4s.MediaRange
import org.http4s.Method
import org.http4s.Response
import org.http4s.{Request as Http4sRequest}

import scala.Tuple.Append

trait Request[A] {
  def extract(request: Http4sRequest[IO]): IO[Option[A]]

  def handle(f: A => IO[Response[IO]]): krop.Route =
    krop.Route.liftRoutes(
      Kleisli[OptionT[IO, *], Http4sRequest[IO], Response[IO]](request =>
        OptionT(extract(request).flatMap(maybePE => maybePE.traverse(f)))
      )
    )
}
object Request {
  def delete: PathRequest[EmptyTuple] =
    PathRequest(method = Method.DELETE, Path.root)

  def get: PathRequest[EmptyTuple] =
    PathRequest(method = Method.GET, Path.root)

  def post: PathRequest[EmptyTuple] =
    PathRequest(method = Method.POST, Path.root)

  def put: PathRequest[EmptyTuple] =
    PathRequest(method = Method.PUT, Path.root)

  def method(method: Method): PathRequest[EmptyTuple] =
    PathRequest(method = method, Path.root)
}

/** A [[krop.route.Request]] that only specifies a method and a
  * [[krop.route.Path]]. The simplest possible [[krop.route.Request]].
  */
final case class PathRequest[P <: Tuple](
    method: Method,
    path: Path[P]
) extends Request[P] {
  def withEntity[E](entity: EntityDecoder[IO, E]): PathEntityRequest[P, E] =
    PathEntityRequest(this, entity)

  def withMethod(method: Method): Request[P] =
    this.copy(method = method)

  def withPath[P2 <: Tuple](path: Path[P2]): Request[P2] =
    this.copy(path = path)

  def extract(request: Http4sRequest[IO]): IO[Option[P]] = {
    IO.pure(
      Option
        .when(request.method == method)(())
        .flatMap(_ => path.extract(request.pathInfo))
    )
  }

}

final case class PathEntityRequest[P <: Tuple, E](
    pathRequest: PathRequest[P],
    decoder: EntityDecoder[IO, E]
) extends Request[Tuple.Append[P, E]] {
  def extract(request: Http4sRequest[IO]): IO[Option[Append[P, E]]] = {
    given EntityDecoder[IO, E] = decoder
    pathRequest
      .extract(request)
      .flatMap(maybePath =>
        maybePath match {
          case None        => IO.pure(None)
          case Some(value) => request.as[E].map(e => Some(value :* e))
        }
      )
  }
}
