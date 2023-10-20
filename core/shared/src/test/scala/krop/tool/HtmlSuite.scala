package krop.tool

import munit.FunSuite

class HtmlSuite extends FunSuite {
  test("HTML quoting correctly quotes reserved characters") {
    val tests = List("<>" -> "&lt;&gt;", "&" -> "&amp;", "\"" -> "&quot;")

    tests.foreach { case (in, out) => assertEquals(Html.quote(in), out) }
  }
}
