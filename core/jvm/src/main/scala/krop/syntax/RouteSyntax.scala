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

package krop.syntax

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import krop.Application
import krop.route.JvmResponse
import krop.route.Route
import krop.tool.NotFound
import org.http4s.HttpRoutes

trait RouteSyntax {
  extension (route: Route) {

    /** Convert this route into an [[krop.Application]] that first tries this
      * route and, if the route fails to match, passes the request to the `app`
      * Application.
      */
    def otherwise(app: Application): Application =
      app.copy(route = route.orElse(app.route))

    /** Convert this [[krop.Route.Route]] into an [[krop.Application]] by
      * responding to all unmatched requests with a NotFound (404) response.
      */
    def otherwiseNotFound: Application =
      route.otherwise(NotFound.notFound)

    /** Convert to the representation used by http4s */
    def toHttpRoutes: IO[HttpRoutes[IO]] =
      route.routes.foldLeftM(HttpRoutes.empty[IO])((accum, atom) =>
        atomToHttpRoutes(atom).map(r => accum <+> r)
      )
  }

  import Route.Atomic
  private def atomToHttpRoutes[P <: Tuple, E <: Tuple, O](
      atom: Atomic[P, E, O]
  ): IO[HttpRoutes[IO]] = {
    val jvmResponse = JvmResponse.fromResponse(atom.response)
    IO.pure(
      Kleisli(req =>
        OptionT(
          atom.request
            .extract(req)
            .flatMap(maybeIn =>
              maybeIn.traverse(in =>
                atom.handler(in).flatMap(out => jvmResponse.respond(req, out))
              )
            )
        )
      )
    )
  }
}
