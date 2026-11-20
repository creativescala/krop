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

import cats.data.Chain
import munit.FunSuite
import org.http4s.UrlForm

import java.util.UUID

class FormCodecSuite extends FunSuite {
  final case class Person(id: UUID, name: String, age: Int, count: Long)
  val personCodec: FormCodec[Person] = FormCodec.derived[Person]
  val uuid: UUID = UUID.fromString("c333bc2e-4d15-4923-a3c8-404da1b0f6ea")
  val person: Person = Person(id = uuid, name = "Bob", age = 47, count = 12345L)

  test("encoding of simple case class FormCodec works as expected") {

    val urlForm = personCodec.encode(person)

    assertEquals(urlForm.get("id"), Chain(uuid.toString))
    assertEquals(urlForm.get("name"), Chain("Bob"))
    assertEquals(urlForm.get("age"), Chain("47"))
    assertEquals(urlForm.get("count"), Chain("12345"))
  }

  test("decoding of simple case class FormCodec works as expected") {
    val either =
      personCodec.decode(
        UrlForm(
          "id" -> uuid.toString,
          "name" -> "Bob",
          "age" -> "47",
          "count" -> "12345"
        )
      )

    assertEquals(either, Right(person))
  }
}
