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
import org.http4s.Request as Http4sRequest
import org.http4s.Uri
import org.http4s.implicits.*

class RequestSuite extends CatsEffectSuite {
  val simpleRequest = Request.get(Path.root)
  test("simple request matches GET /") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    simpleRequest.parse(request)(using Raise.toOption).map(_.isDefined).assert
  }

  test("simple request doesn't match PUT /") {
    val request =
      Http4sRequest(method = Method.PUT, uri = uri"http://example.org/")

    simpleRequest.parse(request)(using Raise.toOption).map(_.isEmpty).assert
  }
}
