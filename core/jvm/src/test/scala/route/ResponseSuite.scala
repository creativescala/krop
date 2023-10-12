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

import munit.CatsEffectSuite
import org.http4s.Method
import org.http4s.Uri
import org.http4s.implicits.*
import org.http4s.{Request as Http4sRequest}

class ResponseSuite extends CatsEffectSuite {
  val staticResourceResponse =
    JvmResponse.fromResponse(Response.staticResource("/krop/assets/"))

  test("static resource response succeeds when resource exists") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    staticResourceResponse
      .respond(request, "pico.min.css")
      .map(_.status.isSuccess)
      .assert
  }

  test("static resource response fails when resource does not exist") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    staticResourceResponse
      .respond(request, "bogus.css")
      .map(!_.status.isSuccess)
      .assert
  }

  test("static file response succeeds when file exists") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    JvmResponse
      .fromResponse(
        Response
          .staticFile("project/plugins.sbt")
      )
      .respond(request, ())
      .map(_.status.isSuccess)
      .assert
  }
}
