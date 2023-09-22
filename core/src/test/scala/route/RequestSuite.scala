package krop.route

import org.http4s.{Request as Http4sRequest}
import munit.CatsEffectSuite
import org.http4s.Method
import org.http4s.Uri
import org.http4s.implicits.*

class RequestSuite extends CatsEffectSuite {
  val simpleRequest = Request.get

  test("simple request matches GET /") {
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    assert(simpleRequest.extract(request).isDefined)
  }

  test("simple request doesn't match PUT /") {
    val request =
      Http4sRequest(method = Method.PUT, uri = uri"http://example.org/")

    assert(simpleRequest.extract(request).isEmpty)
  }
}
