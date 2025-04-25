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

package krop.route

import munit.FunSuite

class SeqStringCodecSuite extends FunSuite {
  def assertDecodingInvertible[A](
      codec: SeqStringCodec[A],
      input: Seq[String],
      output: A
  )(using munit.Location) = {
    val decoded = codec.decode(input)
    assertEquals(decoded, Right(output))

    val encoded = codec.encode(output)
    assertEquals(encoded, input)
  }

  test("SeqStringCodec.separatedString") {
    val comma = SeqStringCodec.separatedString(",")
    val ampersand = SeqStringCodec.separatedString("&")

    assertDecodingInvertible(comma, Seq("a", "b", "c"), "a,b,c")
    assertDecodingInvertible(ampersand, Seq("a", "b", "c"), "a&b&c")
  }
}
