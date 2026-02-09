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

import fs2.io.file.Path as Fs2Path
import munit.FunSuite

class ParamSuite extends FunSuite {
  def paramOneDecodesValid[A](param: Param.One[A], values: Seq[(String, A)])(
      using munit.Location
  ) =
    values.foreach { case (str, a) =>
      assertEquals(param.decode(str), Right(a))
    }

  def paramOneDecodesInvalid[A](param: Param.One[A], values: Seq[String])(using
      munit.Location
  ) =
    values.foreach { (str) => assert(param.decode(str).isLeft) }

  def paramAllDecodesValid[A](
      param: Param.All[A],
      values: Seq[(Seq[String], A)]
  )(using
      munit.Location
  ) =
    values.foreach { case (strings, a) =>
      assertEquals(param.decode(strings), Right(a))
    }

  test("Param.one decodes valid parameter") {
    paramOneDecodesValid(
      Param.int,
      Seq(("1" -> 1), ("42" -> 42), ("-10" -> -10))
    )
    paramOneDecodesValid(
      Param.string,
      Seq(
        ("a" -> "a"),
        ("42" -> "42"),
        ("baby you and me" -> "baby you and me")
      )
    )
  }

  test("Param.one fails to decode invalid parameter") {
    paramOneDecodesInvalid(Param.int, Seq("a", " ", "xyz"))
  }

  test("Param.all decodes valid parameters") {
    paramAllDecodesValid(
      Param.seq,
      Seq(Seq() -> Seq(), Seq("a", "b", "c") -> Seq("a", "b", "c"))
    )
    paramAllDecodesValid(
      Param.separatedString(","),
      Seq(Seq() -> "", Seq("a") -> "a", Seq("a", "b", "c") -> "a,b,c")
    )
    paramAllDecodesValid(
      Param.all[Int],
      Seq(
        Seq() -> Seq(),
        Seq("1") -> Seq(1),
        Seq("1", "2", "3") -> Seq(1, 2, 3)
      )
    )
    paramAllDecodesValid(
      Param.fs2Path,
      Seq(
        Seq() -> Fs2Path(""),
        Seq("a") -> Fs2Path("a"),
        Seq("a", "b", "c") -> Fs2Path("a/b/c")
      )
    )
  }
}
