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

import fs2.io.file.{Path as Fs2Path}
import org.http4s.Status

/** A [[krop.route.Response]] produces a [[org.http4s.Response]] given a value
  * of type A and a [[org.http4s.Request]].
  */
enum Response[A] {
  case StaticResource(pathPrefix: String) extends Response[String]
  case StaticDirectory(pathPrefix: Fs2Path) extends Response[Fs2Path]
  case StaticFile(path: String) extends Response[Unit]
  case StatusAndEntity(status: Status, entity: Entity[A]) extends Response[A]
}
object Response {
  def staticResource(pathPrefix: String): Response[String] =
    Response.StaticResource(pathPrefix)

  def staticDirectory(pathPrefix: Fs2Path): Response[Fs2Path] =
    Response.StaticDirectory(pathPrefix)

  def staticFile(path: String): Response[Unit] =
    Response.StaticFile(path)

  def ok[A](entity: Entity[A]) =
    Response.StatusAndEntity(Status.Ok, entity)
}
