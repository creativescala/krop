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
import krop.route.Routes
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
      (route: Routes) => {
        val kropRoutes = route.orElse(KropAssets.kropAssets.toRoutes)

        val liStart = "<li><pre><code>"
        val liEnd = "</code></pre></li>"

        val description = kropRoutes.routes
          .map(_.request.describe)
          .toList
          .mkString(liStart, s"${liEnd}\n${liStart}", liEnd)

        val httpRoutes = kropRoutes.toHttpRoutes

        def html(req: Request[IO]) =
          s"""
          |<!doctype html>
          |<html lang=en>
          |<head>
          |  <meta charset=utf-8>
          |  <link href="/krop/assets/pico.min.css" rel="stylesheet"/>
          |  <title>Krop: Not Found</title>
          |</head>
          |<body>
          |  <main class="container">
          |    <hgroup>
          |      <h1>404 Not Found</h1>
          |      <h4>This page is created by <code>krop.tool.NotFound</code> and will not be shown in production mode</h4>
          |    </hgroup>
          |    <p>The request</p>
          |    <pre><code>${requestToString(req)}</code></pre>
          |    <p>did not match any routes :-{</p>
          |
          |    <h2>Routes</h2>
          |    <p>The available routes are:</p>
          |    <ul>
          |    ${description}
          |    </li>
          |  </main>
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
  val production: Application = Application(routes =>
    routes.toHttpRoutes.map(r =>
      Kleisli((req: Request[IO]) =>
        r.run(req).getOrElseF(IO.pure(Response.notFound.covary[IO]))
      )
    )
  )

  /** The notFound Application tool. */
  val notFound: Application =
    if Mode.mode.isProduction then production else development
}
