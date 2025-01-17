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
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all._
import krop.Application
import krop.KropRuntime
import krop.tool.NotFound
import org.http4s.HttpRoutes

/** [[package.Routes]] are a collection of zero or more [[package.Route]]. */
final class Routes(val routes: Chain[Route[?, ?, ?, ?, ?]]) {

  /** Create a [[package.Routes]] that tries first these routes, and if they
    * fail to match, the route in the given parameter.
    */
  def orElse(that: Route[?, ?, ?, ?, ?]): Routes =
    Routes(this.routes :+ that)

  /** Create a [[package.Routes]] that tries first these routes, and if they
    * fail to match, the routes in the given parameter.
    */
  def orElse(that: Routes): Routes =
    Routes(this.routes ++ that.routes)

  /** Convert these [[package.Routes]] into an [[krop.Application]] that first
    * tries these Routes and, if they fail to match, passes the request to the
    * Application.
    */
  def orElse(app: Application): Application =
    app.copy(routes = this.orElse(app.routes))

  /** Convert these [[package.Routes]] into an [[krop.Application]] by
    * responding to all unmatched requests with a NotFound (404) response.
    */
  def orElseNotFound(using runtime: KropRuntime): Application =
    this.orElse(NotFound.notFound)

  /** Convert to the representation used by http4s */
  def toHttpRoutes(using runtime: KropRuntime): IO[HttpRoutes[IO]] =
    this.routes.foldLeftM(HttpRoutes.empty[IO])((accum, route) =>
      route.toHttpRoutes.map(r => accum <+> r)
    )
}
object Routes {

  /** The empty [[package.Routes]], which don't match any request. */
  val empty: Routes = new Routes(Chain.empty[Route[?, ?, ?, ?, ?]])
}
