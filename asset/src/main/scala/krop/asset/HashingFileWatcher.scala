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
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.Watcher

/** A utility to watch a directory for changes and emit paths and hashes for
  * changed files.
  */
object HashingFileWatcher {

  /** Events produced by the HashingFileWatcher. Paths are always absolute
    * paths.
    */
  enum Event {

    /** Emitted when we encounter a file for the first time (on the initial scan
      * of the directory, or the file has been created) or a file has been
      * modified. This will only be produced for regular files, not directories
      * or other kinds of files.
      */
    case Hashed(file: Path, hash: HexString)

    /** Emitted when a path been deleted. This could refer to a path that is not
      * a regular file, such as a directory, as we cannot tell after the fact
      * what kind of file it was.
      */
    case Deleted(file: Path)
  }

  /** Produces a `Resource` that, when used, will construct an infinite a stream
    * of `Event` for the given directory. If the given path is not a directory
    * the `Resource` will fail with an `IllegalArgumentException`.
    *
    * An `Event` will be produced on startup for all regular files that are
    * found recursively within the directory, and then subsequent events will be
    * produced when files are created, modified, or deleted.
    */
  def watch(directory: Path): Resource[IO, Stream[IO, Event]] = {
    val files = Files.forIO

    def maybeHash(path: Path): IO[Option[Event]] =
      files.isRegularFile(path).flatMap { isFile =>
        if isFile then path.md5Hex.map(hex => Some(Event.Hashed(path, hex)))
        else IO.pure(None)
      }

    val initialize: IO[Stream[IO, Event]] =
      files.isDirectory(directory).map { isDir =>
        if isDir then
          files
            .walk(directory)
            .evalFilter(path => files.isRegularFile(path))
            .evalMap(path => path.md5Hex.map(hex => Event.Hashed(path, hex)))
        else
          Stream.raiseError(
            IllegalArgumentException(
              s"Cannot create a HashingFileWatcher watching $directory as this path is not a directory."
            )
          )
      }

    val watcher: Stream[IO, Event] =
      files
        .watch(directory)
        .evalMapFilter(event =>
          event match {
            case Watcher.Event.Created(path, _)  => maybeHash(path)
            case Watcher.Event.Modified(path, _) => maybeHash(path)
            case Watcher.Event.Deleted(path, _) =>
              IO.pure(Some(Event.Deleted(path)))
            case other =>
              IO.pure(None)
          }
        )

    initialize
      .map(init => init.onComplete(watcher))
      .toResource
  }
}
