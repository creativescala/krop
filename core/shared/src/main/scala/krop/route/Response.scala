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
import fs2.io.file.Path as Fs2Path
import krop.KropRuntime
import org.http4s.StaticFile
import org.http4s.Status
import org.http4s.dsl.io.*
import org.http4s.Request as Http4sRequest
import org.http4s.Response as Http4sResponse

/** A [[krop.route.Response]] produces a [[org.http4s.Response]] given a value
  * of type A and a [[org.http4s.Request]].
  */
sealed trait Response[A] {
  def respond(request: Http4sRequest[IO], value: A)(using
      runtime: KropRuntime
  ): IO[Http4sResponse[IO]]

  def orNotFound: Response[Option[A]] =
    Response.OrNotFound(this)

  def orElse(that: Response[A]): Response[A] =
    Response.OrElse(this, that)

  def sum[O](that: Response[O]): Response[Either[A, O]] =
    Response.Sum(this, that)
}

object Response {
  case class StaticResource(pathPrefix: String) extends Response[String]:
    /** Respond with a resource loaded by the Classloader. The `pathPrefix` is
      * the prefix within the resources where the Classloader will look. E.g.
      * "/krop/assets/". The `String` value is the rest of the resource name.
      * E.g "krop.css".
      */
    override def respond(request: Http4sRequest[IO], fileName: String)(using
        runtime: KropRuntime
    ): IO[Http4sResponse[IO]] =
      val path = pathPrefix ++ fileName
      StaticFile
        .fromResource(path, Some(request))
        .getOrElseF(
          IO(
            runtime.logger.error(
              s"Resource.staticResource couldn't load a resource from path $path."
            )
          ) *> InternalServerError()
        )

  case class OrNotFound[Z](source: Response[Z]) extends Response[Option[Z]]:
    override def respond(request: Http4sRequest[IO], value: Option[Z])(using
        runtime: KropRuntime
    ): IO[Http4sResponse[IO]] =
      value match
        case Some(a) => source.respond(request, a)
        case None    => IO.pure(Http4sResponse.notFound)

  case class OrElse[O](first: Response[O], second: Response[O])
      extends Response[O]:
    override def respond(request: Http4sRequest[IO], value: O)(using
        runtime: KropRuntime
    ): IO[Http4sResponse[IO]] =
      first
        .respond(request, value)
        .orElse(second.respond(request, value))

  case class Sum[I, O](left: Response[I], right: Response[O])
      extends Response[Either[I, O]]:
    override def respond(request: Http4sRequest[IO], value: Either[I, O])(using
        runtime: KropRuntime
    ): IO[Http4sResponse[IO]] =
      value match
        case Left(b)  => left.respond(request, b)
        case Right(a) => right.respond(request, a)

  def staticResource(pathPrefix: String): Response[String] =
    StaticResource(pathPrefix)

  /** Respond with a file loaded from the filesystem. The `pathPrefix` is the
    * directory within the file system where the files will be found. E.g.
    * "/etc/assets/". The `Path` value is the rest of the resource name. E.g
    * "krop.css".
    */
  def staticDirectory(pathPrefix: Fs2Path): Response[Fs2Path] =
    new Response[Fs2Path] {
      def respond(
          request: Http4sRequest[IO],
          fileName: Fs2Path
      )(using runtime: KropRuntime): IO[Http4sResponse[IO]] = {
        import runtime.given

        StaticFile
          .fromPath[IO](pathPrefix / fileName, Some(request))
          .getOrElseF(InternalServerError())
      }
    }

  /** Respond with a file loaded from the filesystem. The `path` is the location
    * of the file. E.g. "/etc/assets/index.html".
    */
  def staticFile(path: String): Response[Unit] =
    new Response[Unit] {
      def respond(
          request: Http4sRequest[IO],
          unit: Unit
      )(using runtime: KropRuntime): IO[Http4sResponse[IO]] = {
        import runtime.given

        val p = fs2.io.file.Path(path)

        StaticFile
          .fromPath[IO](p, Some(request))
          .getOrElseF {
            fs2.io.file.Files.forIO
              .exists(p)
              .map(exists =>
                runtime.logger
                  .error(
                    s"""
                       |Resource.staticFile couldn't load a file from path $path.
                       |
                       |  This path represents ${p.absolute} as an absolute path.
                       |  A file ${
                        if exists then "does" else "does not"
                      } exist at this path.""".stripMargin
                  )
              )
              *>
                InternalServerError()
          }
      }
    }

  def ok[A](entity: Entity[?, A]): Response[A] =
    status(Status.Ok, entity)

  def status[A](status: Status, entity: Entity[?, A]): Response[A] =
    StatusEntityEncodingResponse(status, entity)

  /** A [[krop.route.Response]] that specifies only a HTTP status code and an
    * [[org.http4s.EntityEncoder]].
    */
  final case class StatusEntityEncodingResponse[A](
      status: Status,
      entity: Entity[?, A]
  ) extends Response[A] {
    def respond(
        request: Http4sRequest[IO],
        value: A
    )(using runtime: KropRuntime): IO[Http4sResponse[IO]] =
      IO.pure(
        Http4sResponse(
          status = status,
          headers = entity.encoder.headers,
          entity = entity.encoder.toEntity(value)
        )
      )
  }

}
