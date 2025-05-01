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
import com.augustnagro.magnum.SqlLogger
import com.augustnagro.magnum.magcats

import java.sql.Connection
import javax.sql.DataSource

/** Simplify the magcats.Transcator type by fixing F to IO. */
type Transactor = magcats.Transactor[IO]
object Transactor {

  /** Construct a Transactor
    *
    * @param dataSource
    *   Datasource to be used
    * @param sqlLogger
    *   Logging configuration
    * @param connectionConfig
    *   Customize the underlying JDBC Connections
    * @param maxBlockingThreads
    *   Number of threads in your connection pool. This helps magcats be more
    *   memory efficient by limiting the number of blocking pool threads used.
    *   Not needed if using a virtual-thread based blocking executor (e.g. via
    *   evalOn)
    * @return
    *   IO[Transactor]
    */
  def apply(
      dataSource: DataSource,
      sqlLogger: SqlLogger,
      connectionConfig: Connection => Unit,
      maxBlockingThreads: Int
  ): IO[Transactor] =
    magcats.Transactor.apply[IO](
      dataSource,
      sqlLogger,
      connectionConfig,
      maxBlockingThreads
    )

  /** Construct a Transactor
    *
    * @param dataSource
    *   Datasource to be used
    * @param sqlLogger
    *   Logging configuration
    * @param maxBlockingThreads
    *   Number of threads in your connection pool. This helps magcats be more
    *   memory efficient by limiting the number of blocking pool threads used.
    *   Not needed if using a virtual-thread based blocking executor (e.g. via
    *   evalOn)
    * @return
    *   IO[Transactor]
    */
  def apply(
      dataSource: DataSource,
      sqlLogger: SqlLogger,
      maxBlockingThreads: Int
  ): IO[Transactor] =
    magcats.Transactor.apply[IO](dataSource, sqlLogger, maxBlockingThreads)

  /** Construct a Transactor
    *
    * @param dataSource
    *   Datasource to be used
    * @param maxBlockingThreads
    *   Number of threads in your connection pool. This helps magcats be more
    *   memory efficient by limiting the number of blocking pool threads used.
    *   Not needed if using a virtual-thread based blocking executor (e.g. via
    *   evalOn)
    * @return
    *   IO[Transactor]
    */
  def apply(
      dataSource: DataSource,
      maxBlockingThreads: Int
  ): IO[Transactor] =
    magcats.Transactor.apply[IO](
      dataSource,
      SqlLogger.Default,
      maxBlockingThreads
    )

  /** Construct a Transactor
    *
    * @param dataSource
    *   Datasource to be used
    * @param sqlLogger
    *   Logging configuration
    * @param connectionConfig
    *   Customize the underlying JDBC Connections
    * @return
    *   IO[Transactor]
    */
  def apply(
      dataSource: DataSource,
      sqlLogger: SqlLogger,
      connectionConfig: Connection => Unit
  ): IO[Transactor] =
    magcats.Transactor.apply[IO](dataSource, sqlLogger, connectionConfig)

  /** Construct a Transactor
    *
    * @param dataSource
    *   Datasource to be used
    * @param sqlLogger
    *   Logging configuration
    * @return
    *   IO[Transactor]
    */
  def apply(
      dataSource: DataSource,
      sqlLogger: SqlLogger
  ): IO[Transactor] =
    magcats.Transactor.apply[IO](dataSource, sqlLogger)

  /** Construct a Transactor
    *
    * @param dataSource
    *   Datasource to be used
    * @return
    *   IO[Transactor]
    */
  def apply(
      dataSource: DataSource
  ): IO[Transactor] =
    magcats.Transactor
      .apply[IO](dataSource)
}
