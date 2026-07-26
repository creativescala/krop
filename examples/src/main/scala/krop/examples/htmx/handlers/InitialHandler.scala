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

package krop.examples.htmx.handlers

import cats.effect.IO
import cats.syntax.all.*
import krop.all.*
import krop.examples.htmx.handlers.Parser.*
import krop.examples.htmx.routes.Routes
import krop.examples.htmx.server.SimpleAuthService
import krop.examples.htmx.views.html
import org.http4s.headers.Cookie
import org.typelevel.log4cats.Logger

final case class InitialHandler(
    service: SimpleAuthService[IO]
)(using Logger[IO]):
  private val name = "Personal Development Plan"

  private val defaultPage =
    html.base(name, html.login(None)).toString

  val handler: Handler =
    Routes.index.handleIO: (cookie: Cookie) =>
      cookie.getToken match
        case Some(token) =>
          service
            .findUser(token)
            .map:
              case Some(user) =>
                html
                  .base(name, html.welcome(user.username, token))
                  .toString
              case None =>
                defaultPage
            .recoverWith:
              case ex =>
                Logger[IO]
                  .error(ex)(s"Server error: ${ex.getMessage}")
                  .as(defaultPage)
        case None =>
          defaultPage.pure[IO]
end InitialHandler
