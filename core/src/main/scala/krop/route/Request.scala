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
import org.http4s.Method
import org.http4s.Response
import org.http4s.{Request as Http4sRequest}

final case class Request[A <: Tuple](method: Method, path: Path[A]) {
  def withMethod(method: Method): Request[A] =
    this.copy(method = method)

  def withPath[B <: Tuple](path: Path[B]): Request[B] =
    this.copy(path = path)

  def extract(request: Http4sRequest[IO]): Option[A] =
    if request.method == method then path.extract(request.pathInfo)
    else None

  def handle(f: A => IO[Response[IO]]): krop.Route =
    krop.Route.liftRoutes(
      Kleisli[OptionT[IO, *], Http4sRequest[IO], Response[IO]](request =>
        OptionT(extract(request).traverse(f))
      )
    )
}
object Request {
  def delete: Request[EmptyTuple] =
    Request(method = Method.DELETE, Path.root)

  def get: Request[EmptyTuple] =
    Request(method = Method.GET, Path.root)

  def post: Request[EmptyTuple] =
    Request(method = Method.POST, Path.root)

  def put: Request[EmptyTuple] =
    Request(method = Method.PUT, Path.root)

  def method(method: Method): Request[EmptyTuple] =
    Request(method = method, Path.root)
}
