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
import cats.effect.Resource
import cats.effect.std.MapRef
import fs2.*
import fs2.hashing.*
import fs2.io.file.Files
import fs2.io.file.Path as Fs2Path
import krop.BaseRuntime
import krop.Key
import krop.route.BaseRoute
import krop.route.ClientRoute
import krop.route.Param
import krop.route.Path
import krop.route.Request
import krop.route.Response
import krop.route.ReversibleRoute
import krop.route.RouteHandler
import HashingFileWatcher.Event.{Hashed, Deleted}

import java.util.HexFormat

final class AssetRoute(base: Path[EmptyTuple, EmptyTuple], directory: Fs2Path)
    extends ReversibleRoute[Tuple1[Fs2Path], EmptyTuple],
      ClientRoute[Tuple1[Fs2Path], Array[Byte]],
      BaseRoute {
  val request
      : Request[Tuple1[Fs2Path], Tuple1[Fs2Path], EmptyTuple, Tuple1[Fs2Path]] =
    Request.get(
      base / Param
        .separatedString("/")
        .imap(str => Fs2Path(str))(path => path.toString)
    )

  val response: Response[Fs2Path, Array[Byte]] =
    Response.staticDirectory(directory)

  // val key: Key[???] = Key.unsafe(s"Assets for $directory")

  def build(runtime: BaseRuntime): Resource[IO, RouteHandler] = {
    val files = Files[IO]

    for {
      logger <- runtime.loggerFactory
        .fromName(s"krop-asset-route($directory)")
        .toResource
      isDir <- files.isDirectory(directory).toResource
      _ <-
        (if isDir then
           logger.info("Creating asset route for directory $directory")
         else {
           logger
             .error(
               s"Cannot create an asset route for $directory as this path is not a directory."
             ) >> IO.raiseError(
             IllegalArgumentException(
               s"Cannot create an asset route for $directory as this path is not a directory."
             )
           )
         }).toResource
      // Table maps file name to hex encoded hash
      map <- MapRef[IO, Fs2Path, HexString].toResource
      events <- HashingFileWatcher.watch(directory)
      hasher = FileNameHasher(logger, events, map)
      _ <- hasher.update.background
    } yield ???

    // new RouteHandler {
    //     def run[F[_, _]](request: Http4sRequest[IO])(using
    //         handle: Raise.Handler[F],
    //         runtime: KropRuntime
    //     ): IO[F[ParseFailure, Http4sResponse[IO]]] =
    //       route.request
    //         .parse(request)
    //         .flatMap(extracted =>
    //           Raise
    //             .mapToIO(extracted)(in =>
    //               handler(in).flatMap(out => route.response.respond(request, out))
    //             )
    //         )
    //   }
  }
}
