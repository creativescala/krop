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

class QueryParamSuite extends FunSuite {
  test("Required QueryParam succeeds if first value parses") {
    val qp = QueryParam("id", StringCodec.int)
    assertEquals(
      qp.decode(Map("id" -> List("1"), "name" -> List("Van Gogh"))),
      Right(1)
    )
    assertEquals(
      qp.decode(Map("id" -> List("1", "foobar"), "name" -> List("Van Gogh"))),
      Right(1)
    )
  }

  test("Required QueryParam fails if first value fails to parse") {
    val qp = QueryParam("id", StringCodec.int)
    assertEquals(
      qp.decode(Map("id" -> List("abc"))),
      Left(QueryParseFailure.ValueParsingFailed("id", "abc", "<Int>"))
    )
  }

  test("Required QueryParam fails if no values are found for name") {
    val qp = QueryParam("id", StringCodec.int)
    assertEquals(
      qp.decode(Map("id" -> List())),
      Left(QueryParseFailure.NoValuesForName("id"))
    )
  }

  test("Required QueryParam fails if name does not exist") {
    val qp = QueryParam("id", StringCodec.int)
    assertEquals(
      qp.decode(Map("foo" -> List("1"))),
      Left(QueryParseFailure.NoParameterWithName("id"))
    )
  }

  test("Optional QueryParam succeeds if first value parses") {
    val qp = QueryParam.optional[Int]("id")
    assertEquals(
      qp.decode(Map("id" -> List("1"), "name" -> List("Van Gogh"))),
      Right(Some(1))
    )
    assertEquals(
      qp.decode(Map("id" -> List("1", "foobar"), "name" -> List("Van Gogh"))),
      Right(Some(1))
    )
  }

  test("Optional QueryParam fails if first value fails to parse") {
    val qp = QueryParam.optional[Int]("id")
    assertEquals(
      qp.decode(Map("id" -> List("abc"))),
      Left(QueryParseFailure.ValueParsingFailed("id", "abc", "<Int>"))
    )
  }

  test("Optional QueryParam succeeds if no values are found for name") {
    val qp = QueryParam.optional[Int]("id")
    assertEquals(
      qp.decode(Map("id" -> List())),
      Right(None)
    )
  }

  test("Optional QueryParam succeeds if name does not exist") {
    val qp = QueryParam.optional[Int]("id")
    assertEquals(
      qp.decode(Map("foo" -> List("1"))),
      Right(None)
    )
  }
}
