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

package krop.tool

import cats.effect.IO
import krop.Mode
import org.http4s.Entity
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status

object NotFound {
  def requestToString(request: Request[IO]): String =
    s"${request.method} ${request.uri.path}"

  def development(request: Request[IO]): Response[IO] = {
    val html = s"""
     |<!doctype html>
     |<html lang=en>
     |<head>
     |  <meta charset=utf-8>
     |  <title>Krop: Not Found</title>
     |</head>
     |<body>
     |  <p>The request for<br/>
     |     <code>${requestToString(request)}</code><br/>
     |     didn't match any routes :-(</p>
     |</body>
     |</html>
     """.stripMargin

    Response(status = Status.NotFound, entity = Entity.utf8String(html))
  }

  val production: Response[IO] = Response.notFound

  def notFound(request: Request[IO]): Response[IO] =
    if Mode.mode.isProduction then production else development(request)
}
