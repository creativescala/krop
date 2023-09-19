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
import cats.effect.unsafe.implicits.global
import org.http4s.server.{Server => Http4sServer}

object Server {

  /** A HTTP server that will serve requests when run. */
  opaque type Server = Resource[IO, Http4sServer]
  extension (server: Server) {

    /** Expose the underlying implementation of this type */
    def unwrap: Resource[IO, Http4sServer] =
      server

    /** Convert this server to a Cats Effect IO for more control over how it is
      * run.
      */
    def toIO: IO[Unit] =
      server.unwrap.use(_ => IO.never)

    /** Run this server, using the default Cats Effect thread pool. The server
      * is run synchronously, so this method will only return when the server
      * has finished.
      */
    def run(): Unit =
      toIO.unsafeRunSync()
  }
  object Server {
    def apply(server: Resource[IO, Http4sServer]): Server =
      server

  }
}
