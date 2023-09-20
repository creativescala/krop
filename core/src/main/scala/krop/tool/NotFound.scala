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
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

object NotFound {
  def requestToString(request: Request[IO]): String =
    s"${request.method} ${request.uri.path}"

  def development(request: Request[IO]): IO[Response[IO]] = {
    val html = s"""
     |<!doctype html>
     |<html lang=en>
     |<head>
     |  <meta charset=utf-8>
     |  <link href="/krop/assets/krop.css" rel="stylesheet"/>
     |  <title>Krop: Not Found</title>
     |</head>
     |<body>
     |  <h1>Not Found</h1>
     |  <p>The request for did not match any routes :-(</p>
     |  <p><code>${requestToString(request)}</code></p>
     |</body>
     |</html>
     """.stripMargin

    org.http4s.dsl.io.NotFound(html, `Content-Type`(MediaType.text.html))
  }

  val production: IO[Response[IO]] = IO.pure(Response.notFound)

  def notFound(request: Request[IO]): IO[Response[IO]] =
    if Mode.mode.isProduction then production else development(request)
}
