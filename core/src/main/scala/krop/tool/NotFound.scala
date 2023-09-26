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

import cats.data.Kleisli
import cats.effect.IO
import krop.Application
import krop.Mode
import krop.Route
import krop.Route.Atomic
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

/** This is a tool that displays useful information in development mode if no
  * route matches a request. In production mode it simply returns a 404 Not
  * Found.
  */
object NotFound {
  def requestToString(request: Request[IO]): String =
    s"${request.method} ${request.uri.path}"

  /** The development version of this tool, which returns useful information in
    * the case of an unmatched request.
    */
  val development: Application = {
    val supervisor =
      (route: Route) => {
        val kropRoutes = route.orElse(KropAssets.kropAssets)

        val description = kropRoutes.routes
          .map(r =>
            r match {
              case Atomic.Krop(request, response, handler) => request.describe
              case Atomic.Http4s(description, routes)      => description
            }
          )
          .toList
          .mkString("<p><code>", "</code></p>\n<p><code>", "</code></p>")

        val httpRoutes = kropRoutes.toHttpRoutes

        def html(req: Request[IO]) =
          s"""
          |<!doctype html>
          |<html lang=en>
          |<head>
          |  <meta charset=utf-8>
          |  <link href="/krop/assets/krop.css" rel="stylesheet"/>
          |  <title>Krop: Not Found</title>
          |</head>
          |<body>
          |  <h1>Not Found</h1>
          |  <p>The request</p>
          |  <p><code>${requestToString(req)}</code></p>
          |  <p>did not match any routes :-{</p>
          |  <h2>Routes</h2>
          |  <p>The available routes are:</p>
          |  ${description}
          |</body>
          |</html>
          """.stripMargin

        def response(req: Request[IO]) =
          org.http4s.dsl.io
            .NotFound(html(req), `Content-Type`(MediaType.text.html))

        val app: IO[HttpApp[IO]] =
          httpRoutes.map(r =>
            Kleisli((req: Request[IO]) => r.run(req).getOrElseF(response(req)))
          )

        app
      }

    Application(supervisor)
  }

  /** The production version of this tool, which returns NotFound to every
    * request.
    */
  val production: Application = Application.notFound

  /** The notFound Application tool. */
  val notFound: Application =
    if Mode.mode.isProduction then production else development
}
