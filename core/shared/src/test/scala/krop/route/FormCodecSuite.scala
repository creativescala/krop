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

class FormCodecSuite extends FunSuite {
  final case class Person(name: String, age: Int)
  val personCodec: FormCodec[Person] = FormCodec.derived[Person]

  test("encoding of simple case class FormCodec works as expected") {
    val person = Person(name = "Bob", age = 47)
    val urlForm = personCodec.encode(person)

    assertEquals(urlForm.get("name"), Chain("Bob"))
    assertEquals(urlForm.get("age"), Chain("47"))
  }

  test("decoding of simple case class FormCodec works as expected") {
    val either =
      personCodec.decode(UrlForm("name" -> "Bob", "age" -> "47"))

    assertEquals(either, Right(Person("Bob", 47)))
  }
}
