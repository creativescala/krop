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

import cats.effect.IO
import krop.KropRuntime
import krop.raise.Raise
import org.http4s.Request as Http4sRequest
import org.http4s.Response as Http4sResponse

/** A RouteHandler is what actually handles an HTTP request and produces an HTTP
  * response.
  */
trait RouteHandler {

  /** Run this RouteHandler on the given request, producing a response or
    * possibly failing.
    */
  def run[F[_, _]](request: Http4sRequest[IO])(using
      handle: Raise.Handler[F],
      runtime: KropRuntime
  ): IO[F[ParseFailure, Http4sResponse[IO]]]
}
