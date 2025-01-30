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
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import krop.Application
import krop.KropRuntime
import krop.tool.NotFound
import org.http4s.HttpRoutes

/** [[package.Handlers]] are a collection of zero or more [[package.Handler]].
  */
final class Handlers(val handlers: Chain[Handler[?, ?]]) {

  /** Create a [[package.Handlers]] that tries first these handlers, and if they
    * fail to match, the route in the given parameter.
    */
  def orElse(that: Handler[?, ?]): Handlers =
    Handlers(this.handlers :+ that)

  /** Create a [[package.Handlers]] that tries first these handlers, and if they
    * fail to match, the handlers in the given parameter.
    */
  def orElse(that: Handlers): Handlers =
    Handlers(this.handlers ++ that.handlers)

  /** Convert these [[package.Handlers]] into an [[krop.Application]] that first
    * tries these Handlers and, if they fail to match, passes the request to the
    * Application.
    */
  def orElse(app: Application): Application =
    app.copy(handlers = this.orElse(app.handlers))

  /** Convert these [[package.Handlers]] into an [[krop.Application]] by
    * responding to all unmatched requests with a NotFound (404) response.
    */
  def orElseNotFound: Application =
    this.orElse(NotFound.notFound)

  /** Convert to the representation used by http4s */
  def toHttpRoutes(using runtime: KropRuntime): HttpRoutes[IO] =
    this.handlers.foldLeft(HttpRoutes.empty[IO])((accum, handler) =>
      accum <+> handler.toHttpRoutes(using runtime)
    )
}
object Handlers {

  /** The empty [[package.Handlers]], which don't match any request. */
  val empty: Handlers = new Handlers(Chain.empty[Handler[?, ?]])
}
