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
import krop.tool
import scalatags.Text.all.*

object TurboStream {
  val indexRoute =
    Route(Request.get(Path.root), Response.ok(Entity.scalatags)).handle(() =>
      index
    )

  val messageRoute =
    Route(
      Request.post(Path.root / "message").withEntity(Entity.urlForm),
      Response.ok(Entity.scalatags)
    )
      .handle(form =>
        form.get("message").headOption match {
          case None => div()
          case Some(value) =>
            tool.TurboStream
              .stream(
                tool.TurboStream.action.append,
                target := "messages",
                tool.TurboStream.template(p(value))
              )
        }
      )

  val index =
    html(
      head(
        script(
          `type` := "module",
          "import hotwiredTurbo from 'https://cdn.skypack.dev/@hotwired/turbo';"
        )
      ),
      body(
        h1("Turbo Stream Example"),
        tool.TurboStream.source(src := ???),
        div(id := "messages", div(id := "message")),
        form(
          action := messageRoute.pathTo,
          method := "post",
          input(id := "message", name := "message", `type` := "text"),
          input(`type` := "Submit")
        )
      )
    )
}

@main def runTurboStream() =
  ServerBuilder.default
    .withApplication(
      TurboStream.indexRoute.orElse(TurboStream.messageRoute).orElseNotFound
    )
    .run()
