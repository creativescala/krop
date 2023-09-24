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
import fs2.io.file.{Path as Fs2Path}
import org.http4s.EntityEncoder
import org.http4s.StaticFile
import org.http4s.Status
import org.http4s.dsl.io.*
import org.http4s.{Request as Http4sRequest}
import org.http4s.{Response as Http4sResponse}

/** A [[krop.route.Response]] produces a [[org.http4s.Response]] given a value
  * of type A and a [[org.http4s.Request]].
  */
trait Response[A] {

  /** Produce the [[org.http4s.Response]] given a request and the value of type
    * A.
    */
  def respond(request: Http4sRequest[IO], value: A): IO[Http4sResponse[IO]]
}
object Response {

  /** Respond with a resource loaded by the Classloader. The `pathPrefix` is the
    * prefix within the resources where the Classloader will look. E.g.
    * "/krop/assets/". The `String` value is the rest of the resource name. E.g
    * "krop.css".
    */
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

  /** Respond with a file loaded fromt the filesystem. The `pathPrefix` is the
    * prefix within the file system where the files will be found. E.g.
    * "/etc/assets/". The `Path` value is the rest of the resource name. E.g
    * "krop.css".
    */
  def staticFile(pathPrefix: Fs2Path): Response[Fs2Path] =
    new Response[Fs2Path] {
      def respond(
          request: Http4sRequest[IO],
          fileName: Fs2Path
      ): IO[Http4sResponse[IO]] = {
        import krop.Logger.given

        StaticFile
          .fromPath[IO](pathPrefix / fileName, Some(request))
          .getOrElseF(InternalServerError())
      }
    }

  def ok[A](using entityEncoder: EntityEncoder[IO, A]): Response[A] =
    status(Status.Ok)(using entityEncoder)

  def status[A](status: Status)(using
      entityEncoder: EntityEncoder[IO, A]
  ): Response[A] =
    StatusEntityEncodingResponse(status, entityEncoder)

  /** A [[krop.route.Response]] that specifies only a HTTP status code and an
    * [[org.http4s.EntityEncoder]].
    */
  final case class StatusEntityEncodingResponse[A](
      status: Status,
      entityEncoder: EntityEncoder[IO, A]
  ) extends Response[A] {
    def respond(
        request: Http4sRequest[IO],
        value: A
    ): IO[Http4sResponse[IO]] =
      IO.pure(
        Http4sResponse(
          status = status,
          headers = entityEncoder.headers,
          entity = entityEncoder.toEntity(value)
        )
      )
  }

}
