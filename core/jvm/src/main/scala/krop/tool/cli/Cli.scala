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

package krop.tool.cli

import cats.syntax.all.*
import com.comcast.ip4s.Port
import com.monovore.decline.*
import krop.all.port

/** Serve command parameters. */
final case class Serve(port: Port)

/** Migrate command parameters. */
final case class Migrate()

/** Defines the Krop command-line parser. */
object Cli {

  /** Parser for the `serve` command */
  val serveOpts: Opts[Serve] =
    Opts.subcommand("serve", "Serve the web application.") {
      Opts
        .option[Int]("port", "The port to use. Defaults to 8080.", short = "p")
        .orNone
        .map(opt => opt.flatMap(Port.fromInt))
        .map(port => Serve(port.getOrElse(port"8080")))
    }

  /** Parser for the `migrate` command */
  val migrateOpts: Opts[Migrate] =
    Opts
      .subcommand("migrate", "Run the database migrations.") {
        Opts.unit
      }
      .as(Migrate())
}
