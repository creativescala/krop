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

class PassthroughBuilderSuite extends FunSuite {

  val emptyRequest = Request.get(Path.root)
  val stringRequest = Request.get(Path.root / Param.string)

  test("Passthrough works for EmptyTuple => Unit") {
    val builder = Route(emptyRequest, Response.ok(Entity.unit))

    builder.passthrough
  }

  test("Passthrough works for Tuple1") {
    val builder = Route(stringRequest, Response.ok(Entity.text))

    builder.passthrough
  }
}
