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
import org.http4s.Uri
import org.http4s.implicits.*

class PathSuite extends FunSuite {
  val nonCapturingPath = Path.root / "user" / "create"
  val nonCapturingAllPath = Path.root / "assets" / "html" / Segment.all
  val capturingAllPath = Path.root / "assets" / "html" / Param.vector
  val simplePath = Path.root / "user" / Param.int.withName("<userId>") / "view"

  test("Path description is as expected") {
    assertEquals(simplePath.describe, "/user/<userId>/view")
  }

  test("Non-capturing path succeeds with empty tuple") {
    val okUri = uri"http://example.org/user/create"

    assertEquals(nonCapturingPath.extract(okUri.path), Some(EmptyTuple))
  }

  test("Path extracts expected element from http4s path") {
    val okUri = uri"http://example.org/user/1234/view"

    assertEquals(simplePath.extract(okUri.path), Some(1234 *: EmptyTuple))
  }

  test("Path fails when cannot parse element from URI path") {
    val badUri = uri"http://example.org/user/foobar/view"

    assertEquals(simplePath.extract(badUri.path), None)
  }

  test("Path fails when insufficient segments in URI path") {
    val badUri = uri"http://example.org/user/"

    assertEquals(simplePath.extract(badUri.path), None)
  }

  test("Path fails when too many segments in URI path") {
    val badUri = uri"http://example.org/user/1234/view/this/that"

    assertEquals(simplePath.extract(badUri.path), None)
  }

  test("Path with all segment matches all extra segments") {
    val okUri = uri"http://example.org/assets/html/this/that/theother.html"

    assertEquals(nonCapturingAllPath.extract(okUri.path), Some(EmptyTuple))
  }

  test("Path with all param captures all extra segments") {
    val okUri = uri"http://example.org/assets/html/this/that/theother.html"

    assertEquals(
      capturingAllPath.extract(okUri.path),
      Some(Vector("this", "that", "theother.html") *: EmptyTuple)
    )
  }

  test("Path with all segment matches zero or more segments") {
    val zeroUri = uri"http://example.org/assets/html"
    val oneUri = uri"http://example.org/assets/html/example.html"
    val manyUri = uri"http://example.org/assets/html/a/b/c/example.html"

    assertEquals(nonCapturingAllPath.extract(zeroUri.path), Some(EmptyTuple))
    assertEquals(nonCapturingAllPath.extract(oneUri.path), Some(EmptyTuple))
    assertEquals(nonCapturingAllPath.extract(manyUri.path), Some(EmptyTuple))
  }

  test("Path with all param matches zero or more segments") {
    val zeroUri = uri"http://example.org/assets/html"
    val oneUri = uri"http://example.org/assets/html/example.html"
    val manyUri = uri"http://example.org/assets/html/a/b/c/example.html"

    assertEquals(
      capturingAllPath.extract(zeroUri.path),
      Some(Vector.empty[String] *: EmptyTuple)
    )
    assertEquals(
      capturingAllPath.extract(oneUri.path),
      Some(Vector("example.html") *: EmptyTuple)
    )
    assertEquals(
      capturingAllPath.extract(manyUri.path),
      Some(Vector("a", "b", "c", "example.html") *: EmptyTuple)
    )
  }
}
