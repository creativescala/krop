package krop.asset

import cats.effect.IO
import cats.effect.Resource
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.Watcher
import fs2.io.file.Watcher.Event

type Deleted = Deleted.type
case object Deleted

object HashingFileWatcher {
  def watch(
      directory: Path
  ): Resource[IO, Stream[IO, (Path, HexString | Deleted)]] = {
    val files = Files.forIO

    val initialize: IO[Stream[IO, (Path, HexString)]] =
      files.isDirectory(directory).map { isDir =>
        if isDir then
          files
            .walk(directory)
            .evalFilter(path => files.isRegularFile(path))
            .evalMap(path => path.md5Hex.map(hex => path -> hex))
        else
          Stream.raiseError(
            IllegalArgumentException(
              s"Cannot create a HashingFileWatcher watching $directory as this path is not a directory."
            )
          )
      }

    val watcher: Resource[IO, Stream[IO, (Path, HexString | Deleted)]] =
      Watcher
        .default[IO]
        .map(watcher =>
          watcher
            .events()
            .evalMapFilter(event =>
              event match {
                case Event.Created(path, _) =>
                  files
                    .isRegularFile(path)
                    .flatMap(isFile =>
                      if isFile then path.md5Hex.map(hex => Some(path -> hex))
                      else IO.pure(None)
                    )
                case Event.Modified(path, _) =>
                  files
                    .isRegularFile(path)
                    .flatMap(isFile =>
                      if isFile then path.md5Hex.map(hex => Some(path -> hex))
                      else IO.pure(None)
                    )
                case Event.Deleted(path, _) =>
                  // WE can't tell if this is deleted file is a regular file as it no longer exists to test against.
                  IO.pure(Some(path -> Deleted))
                case other =>
                  IO.pure(None)
              }
            )
        )

    watcher.evalMap(watchStream => initialize.map(_ ++ watchStream))
  }
}
