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

import cats.syntax.all.*
import cats.data.Kleisli
import cats.data.EitherNec
import cats.effect.IO
import krop.Application
import krop.KropRuntime
import krop.Mode
import krop.route.Routes
import krop.route.Route
import krop.route.ParseFailure
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import krop.raise.Raise
import cats.data.NonEmptyChain

/** This is a tool that displays useful information in development mode if no
  * route matches a request. In production mode it simply returns a 404 Not
  * Found.
  */
object NotFound {
  def requestToString(request: Request[IO]): String =
    s"${request.method} ${request.uri.path}"

  def routeTemplate(
      route: Route[?, ?, ?, ?, ?, ?],
      reason: ParseFailure
  ): String =
    s"""<li>
       |  <p><pre><code>${Html.quote(route.request.describe)}</code></pre></p>
       |  <details>
       |    <summary>${reason.summary}</summary>
       |    <p>${reason.detail}</p>
       |  </details>
       |</li>""".stripMargin

  /** The development version of this tool, which returns useful information in
    * the case of an unmatched request.
    */
  def development(using runtime: KropRuntime): Application = {
    val supervisor =
      (route: Routes) => {
        val kropRoutes = route.orElse(KropAssets.kropAssets.toRoutes)

        def description(
            routes: NonEmptyChain[(Route[?, ?, ?, ?, ?, ?], ParseFailure)]
        ) =
          routes
            .map { case (route, reason) => routeTemplate(route, reason) }
            .toList
            .mkString("\n")

        def html(
            req: Request[IO],
            routes: NonEmptyChain[(Route[?, ?, ?, ?, ?, ?], ParseFailure)]
        ) =
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
          |    ${description(routes)}
          |    </ul>
          |  </main>
          |</body>
          |</html>
          """.stripMargin

        def response(
            req: Request[IO],
            errors: NonEmptyChain[(Route[?, ?, ?, ?, ?, ?], ParseFailure)]
        ) =
          org.http4s.dsl.io
            .NotFound(html(req, errors), `Content-Type`(MediaType.text.html))

        val app: IO[HttpApp[IO]] = {
          type Annotated = (Route[?, ?, ?, ?, ?, ?], ParseFailure)
          given Raise.Handler[Either] = Raise.toEither

          IO.pure(
            Kleisli { req =>
              val rs = kropRoutes.routes.toList
              // We have at least one route, the Krop routes we added ourselves
              val firstRoute = rs.head
              val results: IO[EitherNec[Annotated, Response[IO]]] =
                firstRoute
                  .run(req)
                  .flatMap(either =>
                    rs.tail.foldLeftM(
                      either.leftMap(e => firstRoute -> e).toEitherNec
                    ) { (result, route) =>
                      result match {
                        case Left(errors) =>
                          // If we have failed we accumulate all the failures to
                          // display to the developer
                          route
                            .run(req)
                            .map(_.leftMap(e => errors :+ (route -> e)))
                        case Right(value) =>
                          // If we have succeeded we keep the first success as our result
                          IO.pure(Right(value))
                      }
                    }
                  )

              results.flatMap(either =>
                either match {
                  case Left(errors)    => response(req, errors)
                  case Right(response) => IO.pure(response)
                }
              )
            }
          )
        }

        app
      }

    Application(supervisor)
  }

  /** The production version of this tool, which returns NotFound to every
    * request.
    */
  def production(using runtime: KropRuntime): Application =
    Application(routes =>
      routes.toHttpRoutes.map(r =>
        Kleisli((req: Request[IO]) =>
          r.run(req).getOrElseF(IO.pure(Response.notFound.covary[IO]))
        )
      )
    )

  /** The notFound Application tool. */
  def notFound(using runtime: KropRuntime): Application =
    if Mode.mode.isProduction then production else development
}
