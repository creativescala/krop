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

import cats.effect.IO
import cats.effect.Resource
import krop.route.Handlers
import org.http4s.HttpApp

/** An [[krop.Application]] produces a response for every HTTP request. Compare
  * to [[krop.router.Handler]], which may not produce a response for some
  * requests.
  *
  * @param handlers
  *   The handlers this Application will test against any incoming request
  * @param supervisor
  *   Responsible for constructing a HttpApp that matches all possible requests,
  *   handling requests that the route doesn't match.
  */
final case class Application(
    handlers: Handlers,
    supervisor: (
        Handlers,
        BaseRuntime
    ) => Resource[IO, WithRuntime[HttpApp[IO]]]
) {
  def toHttpApp(runtime: BaseRuntime): Resource[IO, WithRuntime[HttpApp[IO]]] =
    supervisor(handlers, runtime)
}
object Application {

  /** Construct an [[Application]] with no route and the given supervisor. */
  def apply(
      supervisor: (
          Handlers,
          BaseRuntime
      ) => Resource[IO, WithRuntime[HttpApp[IO]]]
  ): Application =
    Application(Handlers.empty, supervisor)

  // /** Lift an [[org.http4s.HttpApp]] into an [[krop.Application]]. */
  // def liftApp(app: HttpApp[IO]): Application =
  //   Application(
  //     Handlers.empty,
  //     (handlers, runtime) => {
  //       val r = handlers.toHttpRoutes(using runtime)
  //       r.map(httpRoutes =>
  //         Kleisli((req: Request[IO]) =>
  //           httpRoutes.run(req).getOrElseF(app.run(req))
  //         )
  //       )
  //     }
  //   )

  // def lift(f: Request[IO] => IO[Response[IO]]): Application =
  //   Application.liftApp(HttpApp(f))

  /** The Application that returns 404 Not Found to all requests. See
    * [[krop.tool.NotFound]] for details on the implementation.
    */
  val notFound: Application =
    krop.tool.NotFound.notFound
}
