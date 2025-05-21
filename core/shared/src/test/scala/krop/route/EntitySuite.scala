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
import munit.CatsEffectSuite
import org.http4s.Request

final case class Form(int: Int, string: String) derives FormCodec

class EntitySuite extends CatsEffectSuite {
  test("FormCodec encoding is invertible") {
    val entity = Entity.formOf[Form]
    val form = Form(42, "Krop")
    val request = Request[IO]().withEntity(form)(using entity.encoder)

    entity.decoder
      .decode(request, true)
      .fold(
        error => fail(s"Decoding failed with error: $error"),
        value => assertEquals(value, Form(42, "Krop"))
      )
  }
}
