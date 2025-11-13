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
import fs2.hashing.*
import fs2.io.file.*

extension (path: Path) {

  /** Calculate the MD5 hash of a file. */
  def md5: IO[Hash] =
    Files.forIO
      .readAll(path)
      .through(Hashing.forIO.hash(HashAlgorithm.MD5))
      .compile
      .lastOrError

  /** Calculate the MD5 hash of a file as a hexadecimal String */
  def md5Hex: IO[HexString] =
    md5.map(hash => HexString.fromHash(hash))
}
