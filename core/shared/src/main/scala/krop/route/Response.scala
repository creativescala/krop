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
import fs2.Pipe
import fs2.Stream
import fs2.io.file.Path as Fs2Path
import krop.KropRuntime
import org.http4s.Header
import org.http4s.Header.ToRaw
import org.http4s.Headers
import org.http4s.Request as Http4sRequest
import org.http4s.Response as Http4sResponse
import org.http4s.StaticFile as Http4sStaticFile
import org.http4s.Status
import org.http4s.dsl.io.*
import org.http4s.websocket.WebSocketFrame

/** A [[krop.route.Response]] returns a [[org.http4s.Response]] given a value of
  * type R and a [[org.http4s.Request]]. The HTTP response produces a value of
  * type P when decoded.
  */
enum Response[R, P] {
  def respond(request: Http4sRequest[IO], value: R)(using
      runtime: KropRuntime
  ): IO[Http4sResponse[IO]] =
    this match {
      case Contramap(f, response) =>
        response.respond(request, f(value))

      case OrEmpty(success, failure) =>
        value match {
          case Some(a) => success.respond(request, a)
          case None    => failure.respond(request, ())
        }

      case OrElse(success, failure) =>
        value match {
          case Left(a)  => failure.respond(request, a)
          case Right(b) => success.respond(request, b)
        }

      case StaticResource(pathPrefix) =>
        val path = pathPrefix ++ value
        Http4sStaticFile
          .fromResource(path, Some(request))
          .getOrElseF(
            IO(
              runtime.logger.error(
                s"Resource.staticResource couldn't load a resource from path $path."
              )
            ) *> InternalServerError()
          )

      case StaticDirectory(pathPrefix) =>
        import runtime.given

        Http4sStaticFile
          .fromPath[IO](pathPrefix / value, Some(request))
          .getOrElseF(InternalServerError())

      case StaticFile(path) =>
        import runtime.given

        Http4sStaticFile
          .fromPath[IO](path, Some(request))
          .getOrElseF {
            fs2.io.file.Files.forIO
              .exists(path)
              .map(exists =>
                runtime.logger
                  .error(
                    s"""
                       |Resource.staticFile couldn't load a file from path $path.
                       |
                       |  This path represents ${path.absolute} as an absolute path.
                       |  A file ${
                        if exists then "does" else "does not"
                      } exist at this path.""".stripMargin
                  )
              )
              *> InternalServerError()
          }

      case StatusEntityEncoding(status, entity) =>
        IO.pure(
          Http4sResponse(
            status = status,
            headers = entity.encoder.headers,
            entity = entity.encoder.toEntity(value)
          )
        )

      case WithHeader(source, headers) =>
        source
          .respond(request, value)
          .map(response => response.withHeaders(headers))

      case WebSocket =>
        val (send, receive) = value
        runtime.webSocketBuilder.build(send, receive)
    }

  def contramap[A](f: A => R): Response[A, P] =
    Contramap(f, this)

  /** Produce this `Response` if given `Some[A]`, otherwise produce a 404
    * `Response`.
    */
  def orNotFound: Response[Option[R], Option[P]] =
    OrEmpty(this, Response.status(Status.NotFound, Entity.unit))

  /** Produce this `Response` if given `Right[A]`, otherwise produce a 404
    * `Response` with entity given by `Left[B]`.
    */
  def orNotFound[R2, P2](
      entity: Entity[R2, P2]
  ): Response[Either[R2, R], Either[P2, P]] =
    OrElse(this, Response.status(Status.NotFound, entity))

  /** Produce this `Response` if given `Right[A]`, otherwise produce that
    * `Response` given `Left[B]`.
    *
    * Usually used for error handling, where that `Response` is the error case.
    * For this reason we specify the successful `Right` case first.
    */
  def orElse[R2, P2](
      that: Response[R2, P2]
  ): Response[Either[R2, R], Either[P2, P]] =
    OrElse(this, that)

  /** Add headers to this Response. The headers can be any form that
    * [[org.http4s.Header.ToRaw]] accepts, which is:
    *
    *   - A value of type `A` which has a `Header[A]` in scope
    *   - A (name, value) pair of `String`, which is treated as a `Recurring`
    *     header
    *   - A `Header.Raw`
    *   - A `Foldable` (`List`, `Option`, etc) of the above.
    */
  def withHeader(header: ToRaw, headers: ToRaw*): Response[R, P] =
    WithHeader(this, Headers(header.values ++ headers.flatMap(_.values)))

  case Contramap[A, R, P](f: A => R, response: Response[R, P])
      extends Response[A, P]
  case OrEmpty[R, P](success: Response[R, P], failure: Response[Unit, Unit])
      extends Response[Option[R], Option[P]]
  case OrElse[R1, P1, R2, P2](
      success: Response[R1, P1],
      failure: Response[R2, P2]
  ) extends Response[Either[R2, R1], Either[P2, P1]]
  case StaticResource(pathPrefix: String) extends Response[String, Array[Byte]]
  case StaticDirectory(pathPrefix: Fs2Path)
      extends Response[Fs2Path, Array[Byte]]
  case StaticFile(path: Fs2Path) extends Response[Unit, Array[Byte]]
  case StatusEntityEncoding(status: Status, entity: Entity[R, P])
  case WithHeader(source: Response[R, P], header: Headers)
  case WebSocket
      extends Response[
        (Stream[IO, WebSocketFrame], Pipe[IO, WebSocketFrame, Unit]),
        Array[Byte]
      ]
}
object Response {

  /** Respond with a resource loaded by the Classloader. The `pathPrefix` is the
    * prefix within the resources where the Classloader will look. E.g.
    * "/krop/assets/". The `String` value is the rest of the resource name. E.g
    * "krop.css".
    */
  def staticResource(pathPrefix: String): Response[String, Array[Byte]] =
    Response.StaticResource(pathPrefix)

  /** Respond with a file loaded from the filesystem. The `pathPrefix` is the
    * directory within the file system where the files will be found. E.g.
    * "/etc/assets/". The `Path` value is the rest of the resource name. E.g
    * "krop.css".
    */
  def staticDirectory(pathPrefix: Fs2Path): Response[Fs2Path, Array[Byte]] =
    Response.StaticDirectory(pathPrefix)

  /** Respond with a file loaded from the filesystem. The `path` is the location
    * of the file. E.g. "/etc/assets/index.html".
    */
  def staticFile(path: String): Response[Unit, Array[Byte]] =
    Response.StaticFile(fs2.io.file.Path(path))

  def badRequest[R, P](entity: Entity[R, P]): Response[R, P] =
    status(Status.BadRequest, entity)

  def notFound[R, P](entity: Entity[R, P]): Response[R, P] =
    status(Status.NotFound, entity)

  def ok[R, P](entity: Entity[R, P]): Response[R, P] =
    status(Status.Ok, entity)

  def internalServerError[R, P](entity: Entity[R, P]): Response[R, P] =
    status(Status.InternalServerError, entity)

  def status[R, P](status: Status, entity: Entity[R, P]): Response[R, P] =
    Response.StatusEntityEncoding(status, entity)

  val websocket: Response[
    (Stream[IO, WebSocketFrame], Pipe[IO, WebSocketFrame, Unit]),
    Array[Byte]
  ] =
    Response.WebSocket
}
