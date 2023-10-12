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

import cats.effect.IO
import org.http4s.DecodeResult
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import org.http4s.Media
import org.http4s.MediaRange
import org.http4s.headers.`Content-Type`
import org.http4s.syntax.all.*

/** An Entity describes how to encode data for an HTTP entity, and decode data
  * from an HTTP entity.
  *
  * @tparam A
  *   The type of data that this entity will encode and decode.
  */
final case class Entity[A](
    decoder: EntityDecoder[IO, A],
    encoder: EntityEncoder[IO, A]
) {
  def imap[B](f: A => B)(g: B => A): Entity[B] =
    this.copy(
      decoder = decoder.map(f),
      encoder = encoder.contramap(g)
    )

  def withContentType(tpe: `Content-Type`): Entity[A] =
    this.copy(
      decoder =
        if decoder.consumes.exists(_.satisfies(tpe.mediaType)) then decoder
        else
          new EntityDecoder[IO, A] {
            val consumes: Set[MediaRange] = decoder.consumes + tpe.mediaType
            def decode(m: Media[IO], strict: Boolean): DecodeResult[IO, A] =
              decoder.decode(m, strict)
          }
      ,
      encoder = encoder.withContentType(tpe)
    )
}
object Entity {
  val unit: Entity[Unit] =
    Entity(
      EntityDecoder.decodeBy[IO, Unit](MediaRange.`*/*`)(_ =>
        DecodeResult.success(IO.unit)
      ),
      EntityEncoder.unitEncoder
    )

  val text: Entity[String] =
    Entity(
      EntityDecoder.text[IO],
      EntityEncoder.stringEncoder()
    )

  val html: Entity[String] =
    text.withContentType(`Content-Type`(mediaType"text/html"))
}
