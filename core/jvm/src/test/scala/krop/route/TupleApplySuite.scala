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
    val route = Route(emptyRequest, Response.ok(Entity.text))

    route.handle(() => s"Ok!")
    route.handleIO(() => IO.pure(s"Ok!"))
  }

  test("Type inference works for Tuple1") {
    val route = Route(intRequest, Response.ok(Entity.text))

    route.handle(i => s"$i Ok!")
    route.handleIO(i => IO.pure(s"$i Ok!"))
  }

  test("Type inference works for Tuple2") {
    val route = Route(intStringRequest, Response.ok(Entity.text))

    route.handle((int, string) => s"${int.toString}: ${string}")
    route.handleIO((int, string) => IO.pure(s"${int.toString}: ${string}"))
  }

  test("Conversion works for EmptyTuple") {
    val f = TupleApply.emptyTupleFunction0Apply[String].tuple(() => "Yeah!")

    assertEquals(f.apply(EmptyTuple), "Yeah!")
  }

  test("Conversion works for Tuple1") {
    val f = TupleApply.tuple1Apply[Int, String].tuple(int => int.toString)

    assertEquals(f.apply(Tuple1(42)), "42")
  }
}
