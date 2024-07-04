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
  val nonCapturingPath = Path / "user" / "create"
  val nonCapturingAllPath = Path / "assets" / "html" / Segment.all
  val capturingAllPath = Path / "assets" / "html" / Param.seq
  val simplePath = Path / "user" / Param.int.withName("<userId>") / "view"

  test("Path description is as expected") {
    assertEquals(simplePath.describe, "/user/<userId>/view")
  }

  test("Non-capturing path succeeds with empty tuple") {
    val okUri = uri"http://example.org/user/create"

    assertEquals(nonCapturingPath.parseToOption(okUri), Some(EmptyTuple))
  }

  test("Path parseToOptions expected element from http4s path") {
    val okUri = uri"http://example.org/user/1234/view"

    assertEquals(simplePath.parseToOption(okUri), Some(1234 *: EmptyTuple))
  }

  test("Path fails when cannot parseToOption element from URI path") {
    val badUri = uri"http://example.org/user/foobar/view"

    assertEquals(simplePath.parseToOption(badUri), None)
  }

  test("Path fails when insufficient segments in URI path") {
    val badUri = uri"http://example.org/user/"

    assertEquals(simplePath.parseToOption(badUri), None)
  }

  test("Path fails when too many segments in URI path") {
    val badUri = uri"http://example.org/user/1234/view/this/that"

    assertEquals(simplePath.parseToOption(badUri), None)
  }

  test("Path with all segment matches all extra segments") {
    val okUri = uri"http://example.org/assets/html/this/that/theother.html"

    assertEquals(nonCapturingAllPath.parseToOption(okUri), Some(EmptyTuple))
  }

  test("Path with all param captures all extra segments") {
    val okUri = uri"http://example.org/assets/html/this/that/theother.html"

    assertEquals(
      capturingAllPath.parseToOption(okUri),
      Some(Vector("this", "that", "theother.html") *: EmptyTuple)
    )
  }

  test("Path with all segment matches zero or more segments") {
    val zeroUri = uri"http://example.org/assets/html"
    val oneUri = uri"http://example.org/assets/html/example.html"
    val manyUri = uri"http://example.org/assets/html/a/b/c/example.html"

    assertEquals(nonCapturingAllPath.parseToOption(zeroUri), Some(EmptyTuple))
    assertEquals(nonCapturingAllPath.parseToOption(oneUri), Some(EmptyTuple))
    assertEquals(nonCapturingAllPath.parseToOption(manyUri), Some(EmptyTuple))
  }

  test("Path with all param matches zero or more segments") {
    val zeroUri = uri"http://example.org/assets/html"
    val oneUri = uri"http://example.org/assets/html/example.html"
    val manyUri = uri"http://example.org/assets/html/a/b/c/example.html"

    assertEquals(
      capturingAllPath.parseToOption(zeroUri),
      Some(Vector.empty[String] *: EmptyTuple)
    )
    assertEquals(
      capturingAllPath.parseToOption(oneUri),
      Some(Vector("example.html") *: EmptyTuple)
    )
    assertEquals(
      capturingAllPath.parseToOption(manyUri),
      Some(Vector("a", "b", "c", "example.html") *: EmptyTuple)
    )
  }

  test("Closed path raises exception when adding additional segments") {
    intercept[IllegalStateException](nonCapturingAllPath / "crash")
    intercept[IllegalStateException](capturingAllPath / "crash")
  }

  test("Path.pathTo produces expected path") {
    assertEquals(nonCapturingPath.pathTo, "/user/create")
    assertEquals(nonCapturingPath.pathTo(EmptyTuple), "/user/create")

    assertEquals(nonCapturingAllPath.pathTo, "/assets/html/")
    assertEquals(nonCapturingAllPath.pathTo(EmptyTuple), "/assets/html/")

    assertEquals(
      capturingAllPath.pathTo(Seq("css", "main.css")),
      "/assets/html/css/main.css"
    )
    assertEquals(
      capturingAllPath.pathTo(Tuple1(Vector("css", "main.css"))),
      "/assets/html/css/main.css"
    )

    assertEquals(simplePath.pathTo(1234), "/user/1234/view")
    assertEquals(simplePath.pathTo(Tuple1(1234)), "/user/1234/view")
  }
}
