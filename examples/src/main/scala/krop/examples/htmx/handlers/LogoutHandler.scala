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
import krop.examples.htmx.routes.Routes
import krop.examples.htmx.server.SimpleAuthService
import krop.examples.htmx.views.html
import org.http4s.AuthScheme
import org.http4s.Credentials.Token
import org.http4s.headers.Authorization

final case class LogoutHandler(service: SimpleAuthService[IO]):
  val handler: Handler =
    Routes.logout.handleIO: (authorization: Authorization) =>
      authorization match
        case Authorization(Token(AuthScheme.Bearer, token)) =>
          service
            .findUser(token)
            .map:
              case Some(_) =>
                html.login(none).toString.asRight.some
              case None =>
                html.login("User not found".some).toString.asLeft.some
        case _ =>
          html
            .login("An authorization error occurred".some)
            .toString
            .asLeft
            .some
            .pure[IO]
end LogoutHandler
