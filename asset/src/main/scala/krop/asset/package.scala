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
