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

package examples

import krop.all.*
import krop.tool.Htmx.*
import scalatags.Text.all.*

val reverseRoute =
  Route(
    Request.get(Path.root / "reverse" / Param.string),
    Response.ok(Entity.scalatags)
  ).handle(str => p(str.reverse))

val index =
  html(
    body(
      h1("Htmx Example"),
      div(id := "reverse"),
      button(
        hxGet := reverseRoute.pathTo("word"),
        hxTarget := "#reverse",
        "Reverse"
      ),
      script(src := "https://unpkg.com/htmx.org@1.9.6")
    )
  )

val indexRoute =
  Route(Request.get(Path.root), Response.ok(Entity.scalatags)).handle(() =>
    index
  )

@main def htmx() =
  ServerBuilder.default
    .withApplication(indexRoute.orElse(reverseRoute).orElseNotFound)
    .run()
