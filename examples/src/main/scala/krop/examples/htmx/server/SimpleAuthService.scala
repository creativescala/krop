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

package krop.examples.htmx.server

import cats.effect.IO
import cats.effect.Ref
import krop.examples.htmx.server.SimpleAuthService.UserInfo

import java.util.UUID

/** !!!Just for the demonstration!!! */
trait SimpleAuthService[F[_]]:
  def findUser(token: String): F[Option[UserInfo]]

  def login(username: String, password: String): F[Option[UserInfo]]

  def newUser(username: String, password: String): F[Either[String, UserInfo]]

object SimpleAuthService:
  final case class UserInfo(username: String, password: String, token: String)

  def make(db: Ref[IO, Vector[UserInfo]]): SimpleAuthService[IO] =
    new SimpleAuthService:
      def findUser(token: String): IO[Option[UserInfo]] =
        db.get.map(_.find(_.token == token))

      def login(username: String, password: String): IO[Option[UserInfo]] =
        db.get.map(
          _.find(user => user.username == username && user.password == password)
        )

      def newUser(
          username: String,
          password: String
      ): IO[Either[String, UserInfo]] = {
        val newUser = UserInfo(username, password, UUID.randomUUID().toString)

        db.get.flatMap:
          case users if users.exists(_.username == username) =>
            IO.pure(Left("A user with such username already exists."))
          case users =>
            db.update(users => newUser +: users).as(Right(newUser))
      }
