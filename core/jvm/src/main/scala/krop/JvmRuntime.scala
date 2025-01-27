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
import org.http4s.server.websocket.WebSocketBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

final class JvmRuntime(val webSocketBuilder: WebSocketBuilder[IO])
    extends KropRuntime {
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  given logger: Logger[IO] = loggerFactory.getLoggerFromName("krop-core")
}
object JvmRuntime {
  def apply: IO[JvmRuntime] =
    WebSocketBuilder[IO].map(builder => new JvmRuntime(builder))
}
