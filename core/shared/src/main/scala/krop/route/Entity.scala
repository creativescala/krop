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
import scalatags.Text.TypedTag

/** Type alias for an Entity where the decoded and encoded type are the same. */
type InvariantEntity[A] = Entity[A, A]

/** An Entity describes how to decode data from an HTTP entity, and encode data
  * to an HTTP entity.
  *
  * @tparam D
  *   The type of data that this entity will decode from a request
  * @tparam E
  *   The type of data that this entity will encode in a response
  */
final case class Entity[D, E](
    decoder: EntityDecoder[IO, D],
    encoder: EntityEncoder[IO, E]
) {

  /** Transform the decoded request data with the given function. */
  def map[D2](f: D => D2): Entity[D2, E] =
    this.copy(
      decoder = decoder.map(f)
    )

  /** Transform the encoded response data with the given function. */
  def contramap[E2](f: E2 => E): Entity[D, E2] =
    this.copy(
      encoder = encoder.contramap(f)
    )

  def withContentType(tpe: `Content-Type`): Entity[D, E] =
    this.copy(
      decoder =
        if decoder.consumes.exists(_.satisfies(tpe.mediaType)) then decoder
        else
          new EntityDecoder[IO, D] {
            val consumes: Set[MediaRange] = decoder.consumes + tpe.mediaType
            def decode(m: Media[IO], strict: Boolean): DecodeResult[IO, D] =
              decoder.decode(m, strict)
          }
      ,
      encoder = encoder.withContentType(tpe)
    )
}
object Entity {
  val unit: InvariantEntity[Unit] =
    Entity(
      EntityDecoder.decodeBy[IO, Unit](MediaRange.`*/*`)(_ =>
        DecodeResult.success(IO.unit)
      ),
      EntityEncoder.unitEncoder
    )

  val text: InvariantEntity[String] =
    Entity(
      EntityDecoder.text[IO],
      EntityEncoder.stringEncoder()
    )

  val html: InvariantEntity[String] =
    text.withContentType(`Content-Type`(mediaType"text/html"))

  val scalatags: Entity[String, TypedTag[String]] =
    html.contramap(tags => "<!DOCTYPE html>" + tags.toString)
}
