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
import cats.effect.IO
import cats.effect.Resource
import krop.Application
import krop.BaseRuntime
import krop.KropRuntime
import krop.raise.Raise
import org.http4s.Request as Http4sRequest
import org.http4s.Response as Http4sResponse

/** A [[krop.route.Handler]] describes how to build an endpoint that can parse a
  * request and produce a response. It combines a [[krop.request.Route]] with
  * the business logic that handles the request and produces the response.
  *
  * A Handler is the basic unit for building a web service. The majority of the
  * service will consist of handlers, with a final catch-all to deal with any
  * requests that are not handled by any of the handlers.
  *
  * A Handler is a description, which means it can build a
  * [[krop.route.RouteHandler]] that does the actual work. This is similar to
  * how `IO` is a description of a program, that is only run when we call the
  * appropriate methods. When the Handler builds the RouteHandler it can also
  * create any resources that are needed to do its work.
  *
  * @tparam I
  *   The type of all the values extracted from the request.
  * @tparam R
  *   The type of the value used to build the [[krop.route.Response]].
  */
trait Handler {

  /** To allow introspection, the handler must provide the [[krop.route.Route]]
    * it works with.
    */
  def route: Route[?, ?, ?, ?, ?]

  /** Try this Handler. If it fails to match, pass control to the given
    * [[krop.Application]].
    */
  def orElse(that: Application): Application =
    this.toHandlers.orElse(that)

  /** Try this Handler. If it fails to match, pass control to the given
    * [[package.Route]].
    */
  def orElse(that: Handler): Handlers =
    this.orElse(that.toHandlers)

  /** Try this Handler. If it fails to match, pass control to the
    * [[package.Handlers]].
    */
  def orElse(that: Handlers): Handlers =
    Handlers(this +: that.handlers)

  def toHandlers: Handlers =
    Handlers(Chain(this))

    /** Convert this Handler in a [[kropu.route.RouteHandler]] that can process
      * an HTTP request and produce a HTTP response. The Handler can also create
      * a resource, possibly registered on the provided [[krop.KropRuntime]],
      * which will last for the life-time of the server.
      */
  def build(runtime: BaseRuntime): Resource[IO, RouteHandler]

}
object Handler {

  /** Implementation of the common case when a Handler is a container of a Route
    * and a handler function. It also a RouteHandler.
    */
  private final class BasicHandler[I <: Tuple, R](
      val route: Route[?, ?, I, ?, R],
      handler: I => IO[R]
  ) extends Handler,
        RouteHandler { self =>
    def build(runtime: BaseRuntime): Resource[IO, RouteHandler] =
      Resource.eval(IO.pure(self))

    def run[F[_, _]](request: Http4sRequest[IO])(using
        handle: Raise.Handler[F],
        runtime: KropRuntime
    ): IO[F[ParseFailure, Http4sResponse[IO]]] =
      route.request
        .parse(request)
        .flatMap(extracted =>
          Raise
            .mapToIO(extracted)(in =>
              handler(in).flatMap(out => route.response.respond(request, out))
            )
        )
  }

  /** Construct a [[krop.route.Handler]] from a [[krop.route.Route]] and a
    * handler function.
    */
  def apply[I <: Tuple, R](
      route: Route[?, ?, I, ?, R],
      handler: I => IO[R]
  ): Handler = BasicHandler(route, handler)
}
