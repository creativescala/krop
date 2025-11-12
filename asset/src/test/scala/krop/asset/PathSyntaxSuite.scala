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
