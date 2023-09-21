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
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status

/** An [[krop.Application]] produces a response for every HTTP request. Compare
  * to [[krop.Route.Route]], which may not produce a response for some requests.
  */
final case class Application(unwrap: IO[HttpApp[IO]])
object Application {

  /** Lift an [[org.http4s.HttpApp]] into an [[krop.Application]]. */
  def of(app: HttpApp[IO]): Application =
    Application(IO.pure(app))

  /** The Application that returns 404 Not Found to all requests. See
    * [[krop.tool.NotFound]] for an alternative the works differently in
    * development mode.
    */
  val notFound: Application =
    Application.of(HttpApp.notFound[IO])
}
