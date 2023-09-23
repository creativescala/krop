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
import org.http4s.StaticFile
import org.http4s.dsl.io.*
import org.http4s.{Request as Http4sRequest}
import org.http4s.{Response as Http4sResponse}

/** A [[krop.route.Response]] produces an [[org.http4s.Response]] given a value
  * of type A and an [[org.http4s.Request]].
  */
trait Response[A] {

  /** Produce a [[org.http4s.Response]] given a request and a value of type A.
    */
  def respond(request: Http4sRequest[IO], value: A): IO[Http4sResponse[IO]]
}
object Response {
  def staticResource(pathPrefix: String): Response[String] =
    new Response[String] {
      def respond(
          request: Http4sRequest[IO],
          fileName: String
      ): IO[Http4sResponse[IO]] =
        StaticFile
          .fromResource(pathPrefix ++ fileName, Some(request))
          .getOrElseF(InternalServerError())
    }
}
