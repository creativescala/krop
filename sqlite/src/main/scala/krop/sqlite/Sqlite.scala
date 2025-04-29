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
import org.sqlite.SQLiteConfig.JournalMode
import org.sqlite.SQLiteConfig.Pragma
import org.sqlite.SQLiteConfig.SynchronousMode
import org.sqlite.SQLiteDataSource

final class Sqlite private (filename: String, config: SQLiteConfig) {
  def create: Resource[IO, Transactor[IO]] = {
    val dataSource = SQLiteDataSource(config)
    dataSource.setUrl(s"jdbc:sqlite:./${filename}")
    Transactor[IO](dataSource).toResource
  }
}
object Sqlite {
  object config {

    /** A configuration that includes some sensible defaults for web
      * applications:
      *
      *   - Write-ahead Log is turned on
      *   - Sync mode is normal
      *   - Journal size limit is 64MB
      *   - Cache size is 2000 pages (8MB)
      *   - MMAP size is 128MB
      *
      * The `SQLiteConfig` object is mutable, so this method creates a new value
      * each time it is called.
      */
    def default: SQLiteConfig = {
      val config = SQLiteConfig()
      // The values are the defaults that Rails uses, which have been tuned for
      // web applications. They are detailed at
      // https://github.com/rails/rails/pull/49349
      config.setJournalMode(JournalMode.WAL)
      config.setSynchronous(SynchronousMode.NORMAL)
      config.setJournalSizeLimit(64 * 1024 * 1024) // 64MB
      config.setCacheSize(2000) // 8MB assuming 4KB pages
      config.setPragma(Pragma.MMAP_SIZE, (128 * 1024 * 1024).toString) // 128 MB
      config
    }

    /** Return an empty `SQLiteConfig` object.
      *
      * The `SQLiteConfig` object is mutable, so this method creates a new value
      * each time it is called.
      */
    def empty: SQLiteConfig = SQLiteConfig()
  }

  def create(filename: String): Resource[IO, Transactor[IO]] =
    Sqlite(filename, config.default).create

  def create(
      filename: String,
      config: SQLiteConfig
  ): Resource[IO, Transactor[IO]] =
    Sqlite(filename, config).create
}
