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
import fs2.io.file.Path
import munit.CatsEffectSuite

import scala.concurrent.duration.*

class HashingFileWatcherSuite extends CatsEffectSuite {
  val files = Files.forIO

  // Overwrite (truncats) files if they already exist.
  def makeFiles(base: Path, filesAndContent: List[(String, String)]): IO[Unit] =
    Stream
      .emits(filesAndContent)
      .flatMap { case (fileName, content) =>
        val file = base / fileName
        Stream(content).through(files.writeUtf8(file))
      }
      .compile
      .drain

  test("HashingFileWatcher emits hashes of already extant files") {
    files.tempDirectory.use { dir =>
      val create: IO[Unit] =
        makeFiles(dir, List("a.txt" -> "bigcats", "b.txt" -> "littlecats"))

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

  test("HashingFileWatcher emits hashes of changed files") {
    files.tempDirectory.use { dir =>
      val create: IO[Unit] =
        makeFiles(dir, List("a.txt" -> "bigcats", "b.txt" -> "littlecats"))

      // The number of events emitted is nondeterministic. E.g. sometimes we get
      // two change events for a file change. To work around this we just sample
      // 2s from the Stream and then close it. This should be enough time for
      // all the events we're looking for to come through.
      val fileHashes: IO[List[(Path, HexString)]] =
        HashingFileWatcher.watch(dir).use { stream =>
          stream
            .interruptAfter(2.seconds)
            .compile
            .toList
            .map(_.collect { case HashingFileWatcher.Event.Hashed(file, path) =>
              file -> path
            })
        }

      val overwrite: IO[Unit] =
        makeFiles(
          dir,
          List("a.txt" -> "largeaardvarks", "b.txt" -> "petitecapybaras")
        )

      val program =
        // Sleep overwrite for 1s so we don't get a race between it and the watcher initialization
        create >> (fileHashes, IO.sleep(1.second) >> overwrite)
          .parMapN((hashes, _) => hashes)
          .map { hashes =>
            assert(hashes.size == 5)
            assert(hashes.contains(dir / "a.txt" -> "bigcats".md5Hex))
            assert(hashes.contains(dir / "b.txt" -> "littlecats".md5Hex))
            assert(hashes.contains(dir / "a.txt" -> "largeaardvarks".md5Hex))
            assert(hashes.contains(dir / "b.txt" -> "petitecapybaras".md5Hex))
          }

      program
    }
  }
}
