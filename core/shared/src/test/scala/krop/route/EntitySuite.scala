package krop.route

import cats.effect.IO
import org.http4s.{Entity as Http4sEntity}
import org.http4s.Media
import org.http4s.MediaType
import org.http4s.Headers
import org.http4s.Request
import org.http4s.headers.`Content-Type`
import munit.CatsEffectSuite

final case class Form(int: Int, string: String) derives FormCodec

class EntitySuite extends CatsEffectSuite {
  test("FormCodec encoding is invertible") {
    val entity = Entity.formOf[Form]
    val form = Form(42, "Krop")
    val request = Request[IO]().withEntity(form)(entity.encoder)

    entity.decoder
      .decode(request, true)
      .fold(
        error => fail(s"Decoding failed with error: $error"),
        value => assertEquals(value, Form(42, "Krop"))
      )
  }
}
