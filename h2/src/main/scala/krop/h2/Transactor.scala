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

package krop.h2

import cats.effect.IO
import cats.effect.Resource
import doobie.h2.H2Transactor
import doobie.util.*

import scala.concurrent.ExecutionContext

enum DatabaseType {
  case Embedded(name: String, directory: Directory)
  case InMemory(name: String)

  def toJdbcString: String =
    this match {
      case Embedded(name, directory) =>
        directory match {
          case Directory.CurrentWorkingDirectory =>
            s"jdbc:h2:./${name}"
          case Directory.HomeDirectory(path) =>
            s"jdbc:h2:~${path}/${name}"
          case Directory.AbsoluteDirectory(path) =>
            s"jdbc:h2:${path}/${name}"
        }

      case InMemory(name) =>
        s"jdbc:h2:mem:${name};DB_CLOSE_DELAY=-1"
    }
}

enum Directory {
  case CurrentWorkingDirectory

  /** Relative to the current user's home directory. Include a / at the start
    * but not at the end.
    */
  case HomeDirectory(path: String)

  /** An absolute directory. Include a / at the start but not at the end. */
  case AbsoluteDirectory(path: String)
}

final case class Transactor(
    ec: Resource[IO, ExecutionContext],
    username: String,
    password: String,
    database: DatabaseType
) {
  def withEc(ec: Resource[IO, ExecutionContext]): Transactor =
    this.copy(ec = ec)

  def withUsername(username: String): Transactor =
    this.copy(username = username)

  def withPassword(password: String): Transactor =
    this.copy(password = password)

  def withDatabase(database: DatabaseType): Transactor =
    this.copy(database = database)

  def build: Resource[IO, H2Transactor[IO]] =
    for {
      ctxt <- ec
      xa <- H2Transactor.newH2Transactor[IO](
        database.toJdbcString,
        username,
        password,
        ctxt
      )
    } yield xa
}
object Transactor {
  val default: Transactor =
    Transactor(
      ec = ExecutionContexts.fixedThreadPool[IO](32),
      username = "",
      password = "",
      database = DatabaseType.InMemory("default")
    )
}
