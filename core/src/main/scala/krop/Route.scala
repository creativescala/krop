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

package krop

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.all.*
import krop.tool.NotFound
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response

object Route {

  /** A [[krop.Route.Route]] is a function that accepts a request */
  opaque type Route = HttpRoutes[IO]
  extension (route: Route) {

    /** Expose the underlying implementation of this type */
    def unwrap: HttpRoutes[IO] =
      route

    /** Try this route. If it fails to match, try the other route. */
    def and(other: Route): Route =
      route <+> other

    /** Convert this route into an [[krop.Application]] by adding a handler for
      * any unmatched requests.
      */
    def orElse(handler: Request[IO] => Response[IO]): Application =
      Application(
        Kleisli(req => route.unwrap.run(req).getOrElse(handler(req)))
      )

    /** Convert this [[krop.Route.Route]] into an [[krop.Application]] by
      * responding to all unmatched requests with a NotFound (404) response.
      */
    def orNotFound: Application =
      route.orElse(NotFound.notFound)
  }
  object Route {
    def apply(route: HttpRoutes[IO]): Route =
      route
  }
}
