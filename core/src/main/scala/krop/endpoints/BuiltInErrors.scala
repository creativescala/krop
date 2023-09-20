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

package krop.endpoints

import endpoints4s.Invalid
import endpoints4s.algebra
import org.http4s.EntityEncoder
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import scodec.bits.ByteVector

import java.nio.charset.StandardCharsets

/** @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] = {
    val hdr = `Content-Type`(MediaType.application.json)
    EntityEncoder.simple(hdr) { invalid =>
      val s = endpoints4s.ujson.codecs.invalidCodec.encode(invalid)
      ByteVector(s.getBytes(StandardCharsets.UTF_8))
    }
  }

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    clientErrorsResponseEntity.contramap(th => Invalid(th.getMessage))
}
