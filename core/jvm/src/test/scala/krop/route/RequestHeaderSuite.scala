package krop.route

import krop.raise.Raise
import munit.CatsEffectSuite
import org.http4s.Method
import org.http4s.Uri
import org.http4s.implicits.*
import org.http4s.{Request as Http4sRequest}
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import org.http4s.Headers

class RequestHeaderSuite extends CatsEffectSuite {
  test("Ensure header fails if header does not exist") {
    val req = Request
      .get(Path.root)
      .ensureHeader(`Content-Type`(MediaType.application.json))
    val request =
      Http4sRequest(method = Method.GET, uri = uri"http://example.org/")

    req.parse(request)(using Raise.toOption).map(_.isEmpty).assert
  }

  test("Ensure header succeeds if header does exist") {
    val header = `Content-Type`(MediaType.application.json)
    val req = Request.get(Path.root).ensureHeader(header)
    val request = Http4sRequest(
      method = Method.GET,
      uri = uri"http://example.org/",
      headers = Headers(header)
    )

    req.parse(request)(using Raise.toOption).map(_.isDefined).assert
  }
}
