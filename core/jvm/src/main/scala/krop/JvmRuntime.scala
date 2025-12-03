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

package krop

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import org.http4s.server.websocket.WebSocketBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.collection.concurrent.TrieMap

final class JvmKropRuntime(
    base: BaseRuntime,
    resources: Map[Key[?], Any],
    val webSocketBuilder: WebSocketBuilder[IO]
) extends KropRuntime {
  given loggerFactory: LoggerFactory[IO] = base.loggerFactory
  given logger: Logger[IO] = base.logger

  def getResource[V](key: Key[V]): V =
    resources(key).asInstanceOf[V]
}

final class JvmBaseRuntime() extends BaseRuntime {
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  given logger: Logger[IO] = loggerFactory.getLoggerFromName("krop-core")

  /** Resources that will be made available in the KropRuntime. */
  private val stagedResources: TrieMap[Key[?], Resource[IO, Any]] =
    TrieMap.empty

  def stageResource[V](key: Key[V], value: Resource[IO, V]): Unit =
    stagedResources += (key -> value)

  def buildResources: Resource[IO, Map[Key[?], Any]] =
    stagedResources.toSeq.foldM(Map.empty[Key[?], Any]) { (map, kv) =>
      val (key, resource) = kv
      resource.map { v =>
        map + (key -> v)
      }
    }
}
object JvmRuntime {
  val base: JvmBaseRuntime = JvmBaseRuntime()

  def krop(builder: WebSocketBuilder[IO]): JvmKropRuntime =
    JvmKropRuntime(base, Map.empty, builder)
}
