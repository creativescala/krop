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

import cats.Monad
import cats.data.Chain
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import krop.Application
import krop.KropRuntime
import krop.raise.Raise
import org.http4s.HttpRoutes
import org.http4s.Request as Http4sRequest
import org.http4s.Response as Http4sResponse

/** A type alias for handlers that handle a single value from a request. */
type Handler1[I, R] = Handler[Tuple1[I], R]

/** A [[krop.route.Handler]] can process a request and produce a response. A
  * Handler is the basic unit for building a web service. The majority of the
  * service will consist of handlers, with a final catch-all to deal with any
  * requests that are not handled by any of the handlers.
  *
  * @tparam I
  *   The type of all the values extracted from the request.
  * @tparam R
  *   The type of the value used to build the [[krop.route.Response]].
  */
final class Handler[I <: Tuple, R](
    val route: Route[?, ?, I, ?, R],
    val handler: I => IO[R]
) {

  /** Try this Handler. If it fails to match, pass control to the given
    * [[krop.Application]].
    */
  def orElse(that: Application): Application =
    this.toHandlers.orElse(that)

  /** Try this Handler. If it fails to match, pass control to the given
    * [[package.Route]].
    */
  def orElse(that: Handler[?, ?]): Handlers =
    this.orElse(that.toHandlers)

  /** Try this Handler. If it fails to match, pass control to the
    * [[package.Handlers]].
    */
  def orElse(that: Handlers): Handlers =
    Handlers(this +: that.handlers)

  def toHandlers: Handlers =
    Handlers(Chain(this))

  def run[F[_, _]: Raise.Handler](
      req: Http4sRequest[IO]
  )(using
      Monad[F[ParseFailure, *]],
      KropRuntime
  ): IO[F[ParseFailure, Http4sResponse[IO]]] =
    route.request
      .parse(req)
      .flatMap(extracted =>
        Raise
          .mapToIO(extracted)(in =>
            handler(in).flatMap(out => route.response.respond(req, out))
          )
      )

  def toHttpRoutes(using runtime: KropRuntime): HttpRoutes[IO] =
    Kleisli(req =>
      OptionT {
        given Raise.Handler[Raise.ToOption] = Raise.toOption
        this.run(req)
      }
    )
}
