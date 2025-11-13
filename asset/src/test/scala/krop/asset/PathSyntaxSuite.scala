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

import fs2.Stream
import fs2.io.file.Files
import munit.CatsEffectSuite

class PathSyntaxSuite extends CatsEffectSuite {
  val files = Files.forIO

  test("md5Hex returns correct value") {
    files.tempFile.use { file =>
      for {
        _ <- Stream("bigcats").through(files.writeUtf8(file)).compile.drain
        hash <- file.md5Hex
      } yield assertEquals(hash.value, "80fe0e83da4321cca20e0cda8a5f86f8")
    }
  }
}
