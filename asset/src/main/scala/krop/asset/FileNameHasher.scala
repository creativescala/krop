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
import cats.effect.std.MapRef
import fs2.io.file.Path as Fs2Path
import org.typelevel.log4cats.Logger

import HashingFileWatcher.Event.{Hashed, Deleted}

/** Utility the maintains a map of Path to HexString, and provides operations to
  * hash a path, adding a HexString, and unhash a path, removing a HexString
  */
final class FileNameHasher(
    logger: Logger[IO],
    events: fs2.Stream[IO, HashingFileWatcher.Event],
    map: MapRef[IO, Fs2Path, Option[HexString]]
) {

  /** IO that updates the map from events on the events stream. You must arrange
    * for this to run, e.g., by running it in the background.
    */
  val update: IO[Unit] =
    events
      .evalMap(event =>
        event match {
          case Hashed(path, hex) =>
            logger.info(s"Noticed asset $path") >> map.setKeyValue(path, hex)
          case Deleted(path) =>
            logger.info(s"Noticed asset $path has been deleted") >> map
              .unsetKey(
                path
              )
        }
      )
      .compile
      .drain

  def hash(path: Fs2Path): IO[Fs2Path] =
    map(path).get.flatMap(opt =>
      opt match
        case Some(hex) => IO.pure(FileNameHasher.hash(path, hex))
        case None =>
          logger.info(
            s"Was asked to hash path $path but this path was not found in the map of paths."
          ) >> IO.pure(path)
    )

  def unhash(path: Fs2Path): Fs2Path =
    FileNameHasher.unhash(path)
}
object FileNameHasher {
  def hash(path: Fs2Path, hex: HexString): Fs2Path = {
    val str = path.toString
    val ext = path.extName
    val idx = str.lastIndexOf(ext)
    Fs2Path(str.substring(0, idx) ++ "-" ++ hex.value ++ ext)
  }

  def unhash(path: Fs2Path): Fs2Path = {
    val str = path.toString
    val ext = path.extName
    val idx = str.lastIndexOf(ext)
    val baseAndHash = str.substring(0, idx)

    val hashIdx = baseAndHash.lastIndexOf('-')
    // If no hash is found, leave the path alone
    val base =
      if hashIdx == -1 then baseAndHash else baseAndHash.substring(0, hashIdx)

    Fs2Path(s"$base$ext")
  }
}
