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

package krop.examples.htmx.routes

import krop.all.*
import krop.examples.htmx.models.LoginRequest
import org.http4s.Status as HttpStatus
import org.http4s.headers.*

object Routes:
  val index =
    Route(
      Request.get(Path.root).extractHeader[`Cookie`],
      Response.ok(Entity.html)
    )

  val home =
    Route(
      Request.get(Path.root / "home").extractHeader[`Cookie`],
      Response.ok(Entity.html)
    )

  val register =
    Route(
      Request.get(Path.root / "register"),
      Response.ok(Entity.html)
    )

  val login = Route(
    Request
      .post(Path.root / "auth" / "login")
      .withEntity(Entity.jsonOf[LoginRequest]),
    Response
      .ok(Entity.html)
      .orElse(Response.status(HttpStatus.Forbidden, Entity.html))
      .orNotFound
  )

  val newUser = Route(
    Request
      .post(Path.root / "new_user")
      .withEntity(Entity.jsonOf[LoginRequest]),
    Response
      .status(HttpStatus.Created, Entity.html)
      .orElse(Response.status(HttpStatus.Conflict, Entity.html))
      .orNotFound
  )

  val logout = Route(
    Request
      .post(Path.root / "auth" / "logout")
      .extractHeader[Authorization],
    Response
      .ok(Entity.html)
      .orElse(Response.status(HttpStatus.Forbidden, Entity.html))
      .orNotFound
  )

  val assetRoute =
    Route(
      Request.get(Path.root / "asset" / Params.separatedString("/")),
      Response.staticResource("/asset/")
    )
end Routes
