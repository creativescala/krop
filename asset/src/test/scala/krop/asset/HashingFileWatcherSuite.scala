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

import cats.effect.Deferred
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

      val overwrite: IO[Unit] =
        makeFiles(
          dir,
          List("a.txt" -> "largeaardvarks", "b.txt" -> "petitecapybaras")
        )

      val expected =
        List(
          (dir / "a.txt" -> "bigcats".md5Hex),
          (dir / "b.txt" -> "littlecats".md5Hex),
          (dir / "a.txt" -> "largeaardvarks".md5Hex),
          (dir / "b.txt" -> "petitecapybaras".md5Hex)
        )

      // The number of events emitted is nondeterministic. E.g. sometimes we get
      // two change events for a file change. To work around this we keep a
      // running total of the events we're looking for (expected) and halt when
      // we have found them all, or we timeout.
      //
      // MacOS uses a polling implementation, so for this test to run in a
      // reasonable time on MacOS we need to poll somewhat rapidly. See
      // https://bugs.openjdk.org/browse/JDK-7133447
      val fileHashes: IO[List[(Path, HexString)]] =
        for {
          deferred <- Deferred[IO, Either[Throwable, Unit]]
          expected <-
            HashingFileWatcher.watch(dir, 200.milliseconds).use { stream =>
              stream
                .collect { case HashingFileWatcher.Event.Hashed(path, hash) =>
                  path -> hash
                }
                .scan(expected)((expected, hash) =>
                  expected.filterNot(_ == hash)
                )
                .evalMap(expected =>
                  if expected.isEmpty then
                    deferred
                      .complete(Right(()))
                      .as(expected)
                  else IO.pure(expected)
                )
                .interruptWhen(Stream.sleep[IO](4.seconds).as(true))
                .interruptWhen(deferred)
                .compile
                .lastOrError
            }
        } yield expected

      val program =
        // Sleep overwrite so we don't get a race between it and the watcher initialization
        create >> (fileHashes, IO.sleep(250.milliseconds) >> overwrite)
          .parMapN((expected, _) => expected)
          .map { expected =>
            assert(expected.isEmpty)
          }

      program
    }
  }
}
