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
  val simplePath = Path.root / "user" / Param.int.withName("<userId>") / "view"

  test("Path description is as expected") {
    assertEquals(simplePath.describe, "/user/<userId>/view")
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
}
