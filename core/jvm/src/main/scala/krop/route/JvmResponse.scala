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
import org.http4s.dsl.io.*
import org.http4s.{Request as Http4sRequest}
import org.http4s.{Response as Http4sResponse}
import org.http4s.{StaticFile as Http4sStaticFile}

trait JvmResponse[A] {

  /** Produce the [[org.http4s.Response]] given a request and the value of type
    * A.
    */
  def respond(request: Http4sRequest[IO], value: A): IO[Http4sResponse[IO]]
}
object JvmResponse {
  def fromResponse[A](response: Response[A]): JvmResponse[A] = {

    response match {
      case Response.StaticResource(pathPrefix)  => staticResource(pathPrefix)
      case Response.StaticDirectory(pathPrefix) => staticDirectory(pathPrefix)
      case Response.StaticFile(pathPrefix)      => staticFile(pathPrefix)
      case Response.StatusAndEntity(status, entity) => ???
    }
  }

  /** Respond with a file loaded from the filesystem. The `pathPrefix` is the
    * directory within the file system where the files will be found. E.g.
    * "/etc/assets/". The `Path` value is the rest of the resource name. E.g
    * "krop.css".
    */
  def staticDirectory(pathPrefix: Fs2Path): JvmResponse[Fs2Path] =
    new JvmResponse[Fs2Path] {
      def respond(
          request: Http4sRequest[IO],
          fileName: Fs2Path
      ): IO[Http4sResponse[IO]] = {
        import krop.Logger.given

        Http4sStaticFile
          .fromPath[IO](pathPrefix / fileName, Some(request))
          .getOrElseF(InternalServerError())
      }
    }

  /** Respond with a resource loaded by the Classloader. The `pathPrefix` is the
    * prefix within the resources where the Classloader will look. E.g.
    * "/krop/assets/". The `String` value is the rest of the resource name. E.g
    * "krop.css".
    */
  def staticResource(pathPrefix: String): JvmResponse[String] =
    new JvmResponse[String] {
      def respond(
          request: Http4sRequest[IO],
          fileName: String
      ): IO[Http4sResponse[IO]] = {
        val path = pathPrefix ++ fileName
        Http4sStaticFile
          .fromResource(path, Some(request))
          .getOrElseF(
            IO(
              krop.Logger.logger.error(
                s"""
                   |Resource.staticResource couldn't load a resource from path ${path}.
                 """.stripMargin
              )
            ) *> InternalServerError()
          )
      }
    }

  /** Respond with a file loaded from the filesystem. The `path` is the location
    * of the file. E.g. "/etc/assets/index.html".
    */
  def staticFile(path: String): JvmResponse[Unit] =
    new JvmResponse[Unit] {
      def respond(
          request: Http4sRequest[IO],
          unit: Unit
      ): IO[Http4sResponse[IO]] = {
        import krop.Logger.given

        val p = fs2.io.file.Path(path)

        Http4sStaticFile
          .fromPath[IO](p, Some(request))
          .getOrElseF {
            fs2.io.file.Files.forIO
              .exists(p)
              .map(exists =>
                krop.Logger.logger
                  .error(
                    s"""
                       |Resource.staticFile couldn't load a file from path ${path}.
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
}
