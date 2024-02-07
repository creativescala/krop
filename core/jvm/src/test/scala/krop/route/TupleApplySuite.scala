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

import cats.effect.IO
import munit.FunSuite

class TupleApplySuite extends FunSuite {

  val emptyRequest = Request.get(Path.root)
  val intRequest = Request.get(Path / Param.int)
  val intStringRequest = Request.get(Path / Param.int / Param.string)

  test("Type inference works for EmptyTuple") {
    val builder = Route(emptyRequest, Response.ok(Entity.text))

    builder.handle(() => s"Ok!")
    builder.handleIO(() => IO.pure(s"Ok!"))
  }

  test("Type inference works for Tuple1") {
    val builder = Route(intRequest, Response.ok(Entity.text))

    builder.handle(i => s"Ok!")
    builder.handleIO(i => IO.pure(s"Ok!"))
  }

  test("Type inference works for Tuple2") {
    val builder = Route(intStringRequest, Response.ok(Entity.text))

    builder.handle((int, string) => s"${int.toString}: ${string}")
    builder.handleIO((int, string) => IO.pure(s"${int.toString}: ${string}"))
  }
}
