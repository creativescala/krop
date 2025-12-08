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

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Queue
import fs2.Pipe
import fs2.Stream
import krop.all.*
import krop.tool
import org.http4s.UrlForm
import org.http4s.websocket.WebSocketFrame
import scalatags.Text.TypedTag
import scalatags.Text.all.*

object TurboStream extends IOApp {
  final case class Message(content: String)

  def application(queue: Queue[IO, Message]): Application = {
    val indexRoute =
      Route(Request.get(Path.root), Response.ok(Entity.scalatags))

    val messageRoute =
      Route(
        Request.post(Path.root / "message").withEntity(Entity.urlForm),
        Response.ok(Entity.unit)
      )

    val streamRoute =
      Route(
        Request.get(Path.root / "stream"),
        Response.websocket
      )

    val assetRoute =
      Route(
        Request.get(Path.root / "asset" / Param.separatedString("/")),
        Response.staticResource("/asset/")
      )

    val index =
      html(
        head(
          meta(charset := "utf-8"),
          script(
            `type` := "module",
            src := "/asset/turbo-8.0.12.js"
          )
        ),
        body(
          h1("Turbo Stream Example"),
          tool.TurboStream.source(
            src := s"ws://localhost:8080${streamRoute.pathTo}"
          ),
          div(id := "messages", div(id := "message")),
          form(
            action := messageRoute.pathTo,
            method := "post",
            input(id := "message", name := "message", `type` := "text"),
            input(`type` := "Submit")
          )
        )
      )

    val indexController: () => TypedTag[String] =
      () => index

    val messageController: UrlForm => IO[Unit] =
      (form: UrlForm) =>
        form.get("message").headOption match {
          case None => IO.unit
          case Some(value) =>
            queue.offer(Message(value))
        }

    val streamController: () => IO[
      (Stream[IO, WebSocketFrame], Pipe[IO, WebSocketFrame, Unit])
    ] =
      () => {
        val send: Stream[IO, WebSocketFrame] =
          Stream
            .repeatEval(queue.take)
            .map(message =>
              WebSocketFrame.Text(
                tool.TurboStream
                  .stream(
                    tool.TurboStream.action.append,
                    target := "messages",
                    tool.TurboStream.template(p(message.content))
                  )
                  .toString,
                last = false
              )
            )

        val receive: Pipe[IO, WebSocketFrame, Unit] =
          stream =>
            stream.evalMap(frame =>
              IO.print("Stream received frame: ") >> IO.println(frame)
            )

        IO.pure((send, receive))
      }

    val routes =
      indexRoute
        .handle(indexController)
        .orElse(messageRoute.handleIO(messageController))
        .orElse(streamRoute.handleIO(streamController))
        .orElse(assetRoute.passthrough)

    routes.orElseNotFound
  }

  def run(args: List[String]): IO[ExitCode] =
    Queue
      .circularBuffer[IO, Message](8)
      .map(queue => application(queue))
      .flatMap(application =>
        ServerBuilder.default
          .withApplication(application)
          .build
          .toIO
          .as(ExitCode.Success)
      )
}
