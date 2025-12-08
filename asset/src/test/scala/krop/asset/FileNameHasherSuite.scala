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
import fs2.io.file.Path
import munit.CatsEffectSuite

class FileNameHasherSuite extends CatsEffectSuite {
  test(
    "FileNameHasher.hash add given hex string in expected location to path"
  ) {
    val hashed =
      FileNameHasher.hash(Path("/a/b/c.txt"), HexString.unsafeApply("1234"))

    IO(assertEquals(hashed, Path("/a/b/c-1234.txt")))
  }

  test(
    "FileNameHasher.hash adds hash to path without extension"
  ) {
    val hashed =
      FileNameHasher.hash(Path("/a/b/c"), HexString.unsafeApply("1234"))

    IO(assertEquals(hashed, Path("/a/b/c-1234")))
  }

  test(
    "FileNameHasher.unhash removes hex string from path"
  ) {
    val original = Path("/a/b/c.txt")
    val hashed = FileNameHasher.hash(original, HexString.unsafeApply("1234"))
    val unhashed = FileNameHasher.unhash(hashed)

    IO(assertEquals(unhashed, original))
  }

  test(
    "FileNameHasher.unhash removes hex string from path without extension"
  ) {
    val original = Path("/a/b/c")
    val hashed = FileNameHasher.hash(original, HexString.unsafeApply("1234"))
    val unhashed = FileNameHasher.unhash(hashed)

    IO(assertEquals(unhashed, original))
  }

  test(
    "FileNameHasher.unhash does not modify path if hex string not found"
  ) {
    val original = Path("/a/b/c.txt")
    val unhashed = FileNameHasher.unhash(original)

    IO(assertEquals(unhashed, original))
  }
}
