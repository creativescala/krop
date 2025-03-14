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

package krop.sqlite

import cats.effect.IO
import cats.effect.Resource
import com.augustnagro.magnum.magcats.Transactor
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource

final class Sqlite private (filename: String, config: SQLiteConfig) {
  def create: Resource[IO, Transactor[IO]] = {
    val dataSource = SQLiteDataSource(config)
    dataSource.setUrl(s"jdbc:sqlite:./${filename}")
    Transactor[IO](dataSource).toResource
  }
}
object Sqlite {
  def create(filename: String): Resource[IO, Transactor[IO]] =
    Sqlite(filename, SQLiteConfig()).create
}
