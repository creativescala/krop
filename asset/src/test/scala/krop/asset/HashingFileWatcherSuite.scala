package krop.asset

import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import munit.CatsEffectSuite
import cats.effect.IO

class HashingFileWatcherSuite extends CatsEffectSuite {
  test("HashingFileWatcher emits hashes of already extant files") {
    val files = Files.forIO

    files.tempDirectory.use { dir =>
      val create: IO[Unit] =
        Stream("a.txt" -> "bigcats", "b.txt" -> "littlecats")
          .flatMap { case (fileName, content) =>
            val file = dir / fileName
            Stream(content).through(files.writeUtf8(file))
          }
          .compile
          .drain

      val fileHashes: IO[List[(Path, HexString)]] =
        HashingFileWatcher.watch(dir).use { stream =>
          stream
            .take(2)
            .compile
            .toList
            .map(_.collect { case HashingFileWatcher.Event.Hashed(file, path) =>
              file -> path
            })
        }

      val program = create >> fileHashes

      program.map { fileHashes =>
        assert(fileHashes.size == 2)
        assert(
          fileHashes.contains(
            dir / "a.txt" -> "80fe0e83da4321cca20e0cda8a5f86f8"
          )
        )
        assert(
          fileHashes.contains(
            dir / "b.txt" -> "485e307791ace28ccad2df0cfeab31ad"
          )
        )
      }
    }
  }
}
