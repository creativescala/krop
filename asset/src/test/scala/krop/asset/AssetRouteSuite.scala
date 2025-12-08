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

package krop.asset

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path as Fs2Path
import krop.JvmKropRuntime
import krop.JvmRuntime
import krop.route.Path
import munit.CatsEffectSuite
import org.http4s.server.websocket.WebSocketBuilder

class AssetRouteSuite extends CatsEffectSuite {
  val files = Files.forIO

  def makeFiles(
      base: Fs2Path,
      filesAndContent: List[(String, String)]
  ): IO[Unit] =
    Stream
      .emits(filesAndContent)
      .flatMap { case (fileName, content) =>
        val file = base / fileName
        Stream(content).through(files.writeUtf8(file))
      }
      .compile
      .drain

  test("AssetRoute.asset returns correct href") {
    val baseRuntime = JvmRuntime.base
    val path = Path / "assets"
    val resource =
      for {
        dir <- files.tempDirectory
        route = AssetRoute(path, dir.toString)
        _ <- makeFiles(dir, List("a.txt" -> "ocelittle")).toResource
        handler <- route.build(baseRuntime)
        builder <- WebSocketBuilder[IO].toResource
        runtime <- baseRuntime.buildResources.map(r =>
          JvmKropRuntime(baseRuntime, r, builder)
        )
      } yield assertEquals(
        route.asset("a.txt")(using runtime),
        s"${path.pathTo(EmptyTuple)}/a-${"ocelittle".md5Hex}.txt"
      )

    resource.use_
  }
}
