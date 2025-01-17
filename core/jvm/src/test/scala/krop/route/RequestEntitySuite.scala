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

import krop.raise.Raise
import munit.CatsEffectSuite
import org.http4s.Method
import org.http4s.Uri
import org.http4s.implicits._
import org.http4s.{Entity => Http4sEntity}
import org.http4s.{Request => Http4sRequest}

class RequestEntitySuite extends CatsEffectSuite {
  val unitRequest = Request.get(Path.root).withEntity(Entity.unit)
  val textRequest = Request.get(Path.root).withEntity(Entity.text)

  test("Unit request parses empty entity") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    unitRequest
      .parse(request)(using Raise.toOption)
      .map(opt =>
        opt match {
          case Some(Tuple1(())) => true
          case other            => fail(s"Not the expected entity: $other")
        }
      )
      .assert
  }

  test("Unit request unparses unit") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    val unparsed = unitRequest.unparse(Tuple1(()))

    assertEquals(unparsed.method, request.method)
    assertEquals(unparsed.uri.path, request.uri.path)
    assertEquals(unparsed.headers, request.headers)
    assertEquals(unparsed.entity, request.entity)
  }

  test("Text request parses string") {
    val request =
      Http4sRequest(
        method = Method.GET,
        uri = uri"http://example.org/",
        entity = Http4sEntity.utf8String("hello")
      )

    textRequest
      .parse(request)(using Raise.toOption)
      .map(opt =>
        opt match {
          case Some(Tuple1("hello")) => true
          case other                 => fail(s"Not the expected entity: $other")
        }
      )
  }

  test("Text request unparses text") {
    val request =
      Http4sRequest(
        method = Method.GET,
        uri = uri"http://example.org/",
        entity = Http4sEntity.utf8String("hello")
      )

    val unparsed = textRequest.unparse(Tuple1("hello"))

    assertEquals(unparsed.method, request.method)
    assertEquals(unparsed.uri.path, request.uri.path)
    assertEquals(unparsed.headers, request.headers)
    assertEquals(unparsed.entity, request.entity)
  }
}
