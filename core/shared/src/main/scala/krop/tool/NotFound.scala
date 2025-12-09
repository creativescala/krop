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

import cats.data.EitherNec
import cats.data.Kleisli
import cats.data.NonEmptyChain
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import krop.Application
import krop.BaseRuntime
import krop.KropRuntime
import krop.Mode
import krop.WithRuntime
import krop.raise.Raise
import krop.route.BaseRoute
import krop.route.Handler
import krop.route.Handlers
import krop.route.ParseFailure
import krop.route.RouteHandler
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

import scala.collection.mutable

/** This is a tool that displays useful information in development mode if no
  * route matches a request. In production mode it simply returns a 404 Not
  * Found.
  */
object NotFound {
  def requestToString(request: Request[IO]): String =
    s"${request.method} ${request.uri.path}"

  def routeTemplate(
      route: BaseRoute,
      reason: ParseFailure
  ): String =
    s"""<li>
       |  <p><pre><code>${Html.quote(route.request.describe)}</code></pre></p>
       |  <details>
       |    <summary><code>${reason.stage.toString}</code> ${reason.summary}</summary>
       |    <p>${reason.detail}</p>
       |  </details>
       |</li>""".stripMargin

  /** The development version of this tool, which returns useful information in
    * the case of an unmatched request.
    */
  def development(
      handlers: Handlers,
      runtime: BaseRuntime
  ): Resource[IO, WithRuntime[HttpApp[IO]]] = {
    val kropHandlers = handlers.orElse(KropAssets.kropAssets.toHandlers)

    def description(
        handlers: NonEmptyChain[(Handler, ParseFailure)]
    ) =
      handlers
        .map { case (handler, reason) => routeTemplate(handler.route, reason) }
        .toList
        .mkString("\n")

    def notFoundHtml(
        req: Request[IO],
        handlers: NonEmptyChain[(Handler, ParseFailure)]
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
       |    <p>did not match any handlers :-{</p>
       |
       |    <h2>Routes</h2>
       |    <p>The available handlers are:</p>
       |    <ul>
       |    ${description(handlers)}
       |    </ul>
       |  </main>
       |</body>
       |</html>
       """.stripMargin

    def internalErrorHtml(req: Request[IO], exn: Throwable) =
      s"""
       |<!doctype html>
       |<html lang=en>
       |<head>
       |  <meta charset=utf-8>
       |  <link href="/krop/assets/pico.min.css" rel="stylesheet"/>
       |  <title>Krop: Internal Error</title>
       |</head>
       |<body>
       |  <main class="container">
       |    <hgroup>
       |      <h1>500 Internal Error</h1>
       |      <h4>This page is created by <code>krop.tool.NotFound</code> and will not be shown in production mode</h4>
       |    </hgroup>
       |    <p>The request</p>
       |    <pre><code>${requestToString(req)}</code></pre>
       |    <p>caused an exception in the matching route.</p>
       |
       |    <h2>Details</h2>
       |    <p>${exn.getMessage()}</p>
       |    <pre>${exn.getStackTrace().mkString("\n")}</pre>
       |  </main>
       |</body>
       |</html>
       """.stripMargin

    def notFound(
        req: Request[IO],
        errors: NonEmptyChain[(Handler, ParseFailure)]
    ) =
      org.http4s.dsl.io
        .NotFound(
          notFoundHtml(req, errors),
          `Content-Type`(MediaType.text.html)
        )

    def internalError(
        req: Request[IO],
        exn: Throwable
    ) =
      org.http4s.dsl.io
        .InternalServerError(
          internalErrorHtml(req, exn),
          `Content-Type`(MediaType.text.html)
        )

    val app: Resource[IO, WithRuntime[HttpApp[IO]]] = {
      type Annotated = (Handler, ParseFailure)
      given Raise.Handler[Either] = Raise.toEither

      val routeHandlers: Resource[IO, List[(Handler, RouteHandler)]] =
        kropHandlers.handlers.foldRight(
          Resource.eval(IO.pure(List.empty[(Handler, RouteHandler)]))
        ) { (handler, accum) =>
          handler
            .build(runtime)
            .flatMap(routeHandler =>
              accum.map(list => (handler -> routeHandler) :: list)
            )
        }

      routeHandlers.map { list => (runtime: KropRuntime) ?=>
        Kleisli { (req: Request[IO]) =>
          // We have at least one route, the Krop handlers we added ourselves
          val (handler, routeHandler) = list.head
          val results: IO[EitherNec[Annotated, Response[IO]]] =
            routeHandler
              .run(req)
              .flatMap(either =>
                list.tail.foldLeftM(
                  either.leftMap(e => handler -> e).toEitherNec
                ) { (result, pair) =>
                  val (handle, routeHandler) = pair
                  result match {
                    case Left(errors) =>
                      // If we have failed we accumulate all the failures to
                      // display to the developer
                      routeHandler
                        .run(req)
                        .map(_.leftMap(e => errors :+ (handle -> e)))
                    case Right(value) =>
                      // If we have succeeded we keep the first success as our result
                      IO.pure(Right(value))
                  }
                }
              )

          results
            .flatMap(either =>
              either match {
                case Left(errors)    => notFound(req, errors)
                case Right(response) => IO.pure(response)
              }
            )
            .recoverWith(exn => internalError(req, exn))
        }
      }
    }

    app
  }

  /** The production version of this tool, which returns NotFound to every
    * request.
    */
  def production(
      handlers: Handlers,
      runtime: BaseRuntime
  ): Resource[IO, WithRuntime[HttpApp[IO]]] = {
    given Raise.Handler[Raise.ToOption] = Raise.toOption

    // Sneaky mutable code for speed, where no-one can observe it.
    val builder = mutable.ArrayBuilder.make[RouteHandler]

    val resource: Resource[IO, Array[RouteHandler]] =
      handlers.handlers
        .foldLeft(
          Resource.eval(IO.pure(builder))
        ) { (accum, handler) =>
          accum.flatMap(chain =>
            handler.build(runtime).map(routeHandler => builder += routeHandler)
          )
        }
        .map(builder => builder.result())

    val nope = IO.pure(Response.notFound)

    resource.map { routeHandlers => (runtime: KropRuntime) ?=>
      Kleisli((req: Request[IO]) =>
        def loop(idx: Int): IO[Response[IO]] =
          if idx >= routeHandlers.size then nope
          else {
            val routeHandler = routeHandlers(idx)
            routeHandler.run(req).flatMap { opt =>
              if opt.isDefined then IO.pure(opt.get)
              else loop(idx + 1)
            }
          }

        loop(0)
      )
    }
  }

  /** The notFound Application tool. */
  def notFound: Application =
    Application((handlers, runtime) =>
      if Mode.mode.isProduction then production(handlers, runtime)
      else development(handlers, runtime)
    )
}
