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
