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

package krop.examples.htmx

import cats.effect.*
import krop.all.*
import krop.examples.htmx.handlers.*
import krop.examples.htmx.server.SimpleAuthService
import krop.examples.htmx.server.SimpleAuthService.UserInfo
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

val name = "Personal Development Plan"

object Main extends IOApp:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  def application(db: Ref[IO, Vector[UserInfo]]): Application =
    val service: SimpleAuthService[IO] =
      SimpleAuthService.make(db)

    val initialHandler = InitialHandler(service)
    val homeHandler = HomeHandler(service)
    val loginHandler = LoginHandler(service)
    val logoutHandler = LogoutHandler(service)
    val newUserHandler = NewUserHandler(service)
    val assetRoute =
      Route(
        Request.get(Path.root / "asset" / Params.separatedString("/")),
        Response.staticResource("/asset/")
      )

    initialHandler.handler
      .orElse(homeHandler.handler)
      .orElse(RegisterHandler.handler)
      .orElse(loginHandler.handler)
      .orElse(logoutHandler.handler)
      .orElse(newUserHandler.handler)
      .orElse(assetRoute.passthrough)
      .orElse(Application.notFound)

  override def run(args: List[String]): IO[ExitCode] =
    Ref[IO]
      .of(Vector.empty[UserInfo])
      .flatMap: db =>
        ServerBuilder.default
          .withApplication(application(db))
          .build
          .toIO
          .as(ExitCode.Success)
