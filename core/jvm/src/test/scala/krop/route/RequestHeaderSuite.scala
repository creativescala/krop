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
import org.http4s.Headers
import org.http4s.MediaType
import org.http4s.Method
import org.http4s.Uri
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.http4s.{Request as Http4sRequest}

class RequestHeaderSuite extends CatsEffectSuite {
  val jsonContentType = `Content-Type`(MediaType.application.json)
  val jsonRequest = Http4sRequest(
    method = Method.GET,
    uri = uri"http://example.org/",
    headers = Headers(jsonContentType)
  )

  test("Ensure header fails if header does not exist") {
    val req = Request
      .get(Path.root)
      .ensureHeader(`Content-Type`(MediaType.application.json))
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    req.parse(request)(using Raise.toOption).map(_.isEmpty).assert
  }

  test("Ensure header succeeds if header does exist") {
    val req = Request.get(Path.root).ensureHeader(jsonContentType)

    req
      .parse(jsonRequest)(using Raise.toOption)
      .map(opt => assertEquals(opt, Some(EmptyTuple)))
  }

  test("Extract header extracts desired header (by type version)") {
    val req: Request[?, ?, Tuple1[`Content-Type`], ?] =
      Request.get(Path.root).extractHeader[`Content-Type`]

    req
      .parse(jsonRequest)(using Raise.toOption)
      .map(h =>
        h match {
          case None         => fail("Did not parse")
          case Some(header) => assertEquals(header(0), jsonContentType)
        }
      )
  }

  test("Extract header extracts desired header (by value version)") {
    val req: Request[?, ?, Tuple1[`Content-Type`], ?] =
      Request.get(Path.root).extractHeader(jsonContentType)

    req
      .parse(jsonRequest)(using Raise.toOption)
      .map(h =>
        h match {
          case None         => fail("Did not parse")
          case Some(header) => assertEquals(header(0), jsonContentType)
        }
      )
  }
}
