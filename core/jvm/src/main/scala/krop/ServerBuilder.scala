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
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import org.http4s.ember.server.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

/** A description of how to create a [[krop.Server.Server]]. */
final case class ServerBuilder(unwrap: IO[EmberServerBuilder[IO]]) {

  /** Build a [[krop.Server.Server]] from this description. */
  def build: Server =
    Server(unwrap.toResource.flatMap(_.build))

  /** Build a[[krop.Server.Server]] from this description and immediately run
    * it. The server is run synchronously, so this method will only return when
    * the server has finished.
    */
  def run(): Unit = {
    this.build.run()
  }

  /** Set the port on which the server will listen. Use the `port` string
    * context to create a `Port` value.
    *
    * ```
    * ServerBuilder.default.withPort(port"4000")
    * ```
    */
  def withPort(port: Port): ServerBuilder =
    ServerBuilder(unwrap.map(_.withPort(port)))

  /** Set the host address on which the server will listen. Use the `host`
    * string context to create a `Host` value.
    *
    * ```
    * ServerBuilder.default.withHost(host"127.0.0.1")
    * ServerBuilder.default.withHost(host"localhost")
    * ```
    */
  def withHost(host: Host): ServerBuilder =
    ServerBuilder(unwrap.map(_.withHost(host)))

  /** Set the application that the server will run. */
  def withApplication(app: Application): ServerBuilder =
    ServerBuilder(
      for {
        app <- app.toHttpApp
        builder <- unwrap
      } yield builder.withHttpApp(app)
    )
}
object ServerBuilder {
  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val default = ServerBuilder(IO.pure(EmberServerBuilder.default[IO]))
}
