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

/** A [[krop.Route.Route]] is a function that accepts a request and may produce
  * a response.
  */
final case class Route(unwrap: IO[HttpRoutes[IO]]) {

  /** Try this route. If it fails to match, try the other route. */
  def orElse(that: Route): Route =
    Route(
      for {
        l <- this.unwrap
        r <- that.unwrap
      } yield l <+> r
    )

  /** Convert this route into an [[krop.Application]] that first tries this
    * route and, if the route fails to match, passes the request to the `app`
    * Application.
    */
  def otherwise(app: Application): Application =
    Application.lift(req =>
      for {
        r <- unwrap
        a <- app.unwrap
        result <- r.run(req).getOrElseF(a(req))
      } yield result
    )

  /** Convert this [[krop.Route.Route]] into an [[krop.Application]] by
    * responding to all unmatched requests with a NotFound (404) response.
    */
  def otherwiseNotFound: Application =
    this.otherwise(NotFound.notFound)
}
object Route {

  /** Lift an [[org.http4s.HttpRoutes]] into a [[krop.Route]]. */
  def liftRoutes(routes: HttpRoutes[IO]): Route =
    Route(IO.pure(routes))

  /** Lift a partial function into a [[krop.Route]]. */
  def lift(f: PartialFunction[Request[IO], IO[Response[IO]]]): Route =
    Route.liftRoutes(HttpRoutes.of(f))

  /** The empty route, which doesn't match any request. */
  val empty: Route =
    Route.liftRoutes(HttpRoutes.empty[IO])
}
