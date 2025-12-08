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
import fs2.io.file.Files
import fs2.io.file.Path as Fs2Path
import krop.BaseRuntime
import krop.Key
import krop.KropRuntime
import krop.route.BaseRoute
import krop.route.ClientRoute
import krop.route.Handler
import krop.route.InternalRoute
import krop.route.Param
import krop.route.Path
import krop.route.Request
import krop.route.Response
import krop.route.ReversibleRoute
import krop.route.Route
import krop.route.RouteHandler

final class AssetRoute(base: Path[EmptyTuple, EmptyTuple], directory: Fs2Path)
    extends ReversibleRoute[Tuple1[Fs2Path], EmptyTuple],
      ClientRoute[Tuple1[Fs2Path], Array[Byte]],
      InternalRoute[Tuple1[Fs2Path], Fs2Path],
      Handler { self =>
  val request
      : Request[Tuple1[Fs2Path], Tuple1[Fs2Path], EmptyTuple, Tuple1[Fs2Path]] =
    Request.get(
      base / Param
        .separatedString("/")
        .imap(str => Fs2Path(str))(path => path.toString)
    )

  val response: Response[Fs2Path, Array[Byte]] =
    Response.staticDirectory(directory)

  val route: BaseRoute =
    new BaseRoute {
      def request = self.request
      def response = self.response
    }

  private final class Asset(hasher: FileNameHasher) {
    def apply(path: Fs2Path): IO[Fs2Path] =
      hasher.hash(path)
    def apply(path: String): IO[String] =
      apply(Fs2Path(path)).map(_.toString)
  }

  private val key: Key[Asset] = Key.unsafe(s"Assets for ${directory.toString}")
  private val basePath: String = base.pathTo(EmptyTuple)

  /** Given a relative filesystem path to an asset, return a relative path that
    * is suitable for using as a hyperlink, containing the original file plus
    * added hash for cache busting.
    *
    * The relative path parameter is the path on the filesystem where the asset
    * is found. It must be relative to the `directory` this `AssetRoute` was
    * constructed with.
    *
    * The result is the path under which the asset is served. That is, it
    * includes the `pathTo` of this `AssetRoute`'s `base`.
    *
    * This method is only safe to be called once this `AssetRoute` has been
    * built (i.e. the `build` method has been called, and the resulting
    * `Resource` has been used.) This will naturally occur if you use an
    * `AssetRoute` in a Krop application.
    */
  def asset(path: String)(using KropRuntime): String =
    key.get
      .apply(Fs2Path(path))
      .map(p => basePath ++ "/" ++ p.toString)
      .unsafeRunSync()(using cats.effect.unsafe.implicits.global)

  def build(runtime: BaseRuntime): Resource[IO, RouteHandler] = {
    val files = Files[IO]

    val hasher: Resource[IO, FileNameHasher] =
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
        assets = Resource.pure[IO, Asset](Asset(hasher))
        _ = runtime.stageResource(key, assets)
      } yield hasher

    hasher.flatMap(h =>
      (Route(request, response).handle(path => h.unhash(path)).build(runtime))
    )
  }
}
object AssetRoute {
  def apply(base: Path[EmptyTuple, EmptyTuple], directory: String): AssetRoute =
    new AssetRoute(base, Fs2Path(directory))
}
