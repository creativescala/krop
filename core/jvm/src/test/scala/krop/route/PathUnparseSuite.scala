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

class PathUnparseSuite extends FunSuite {
  val rootPath = Path.root
  val nonCapturingPath = Path / "user" / "create"
  val nonCapturingAllPath = Path / "assets" / "html" / Segment.all
  val capturingAllPath = Path / "assets" / "html" / Param.seq
  val simplePath = Path / "user" / Param.int.withName("<userId>") / "view"
  val pathWithQuery = Path / "user" / "view" :? Query[Int]("id")

  test("Root path unparses to expected Uri") {
    assertEquals(
      rootPath.unparse(EmptyTuple),
      Uri(path = Uri.Path.Root)
    )
  }

  test("Non-capturing path unparses to expected Uri") {
    assertEquals(
      nonCapturingPath.unparse(EmptyTuple),
      Uri(path = Uri.Path.Root / "user" / "create")
    )
  }

  test("Non-capturing all path unparses to expected Uri") {
    assertEquals(
      nonCapturingAllPath.unparse(EmptyTuple),
      Uri(path = (Uri.Path.Root / "assets" / "html").addEndsWithSlash)
    )
  }

  test("Capturing all path unparses to expected Uri") {
    assertEquals(
      capturingAllPath.unparse(Seq("style.css") *: EmptyTuple),
      Uri(path = Uri.Path.Root / "assets" / "html" / "style.css")
    )
  }

  test("Capturing path unparses to expected Uri") {
    assertEquals(
      simplePath.unparse(1234 *: EmptyTuple),
      Uri(path = Uri.Path.Root / "user" / "1234" / "view")
    )
  }

  test("Path with query unparses to expected Uri") {
    assertEquals(
      pathWithQuery.unparse(1234 *: EmptyTuple),
      Uri(
        path = Uri.Path.Root / "user" / "view",
        query = org.http4s.Query("id" -> Some("1234"))
      )
    )
  }
}
