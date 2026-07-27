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
import krop.examples.htmx.models.LoginRequest
import krop.examples.htmx.routes.Routes
import krop.examples.htmx.server.SimpleAuthService
import krop.examples.htmx.views.html
import org.typelevel.log4cats.Logger

final case class LoginHandler(
    service: SimpleAuthService[IO]
)(using Logger[IO]):
  val handler: Handler =
    Routes.login.handleIO { (request: LoginRequest) =>
      service
        .login(request.username, request.password)
        .map:
          case Some(user) =>
            html.welcome(user.username, user.token).toString.asRight.some
          case None =>
            html.login("User not found".some).toString.asLeft.some
        .recoverWith:
          case ex =>
            Logger[IO].error(ex)(s"Server error: ${ex.getMessage}").as(none)
    }
end LoginHandler
