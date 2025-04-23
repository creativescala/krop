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
import cats.syntax.all.*
import org.http4s.UrlForm

import scala.compiletime.*
import scala.deriving.*

final case class FormCodec[A](
    decode: UrlForm => Either[Chain[SeqStringDecodeFailure], A],
    encode: A => UrlForm
)

object FormCodec {
  private inline def elementLabels[A <: Tuple]: List[String] =
    inline erasedValue[A] match {
      case _: EmptyTuple => List.empty
      case _: (hd *: tl) =>
        val headLabel = constValue[hd].toString()
        val tailLabels = elementLabels[tl]

        headLabel :: tailLabels
    }

  inline given derived[A](using m: Mirror.Of[A]): FormCodec[A] =
    inline m match {
      case s: Mirror.SumOf[A] =>
        error("Derivation of FormCodecs is not implemented for sum types.")

      case p: Mirror.ProductOf[A] =>
        // We lose type information when we convert to array
        //

        // Array[String]
        val labels =
          constValueTuple[p.MirroredElemLabels].toArray

        // Array[SeqStringCodec[?]]
        val codecs =
          summonAll[Tuple.Map[p.MirroredElemTypes, SeqStringCodec]].toArray

        val decode: UrlForm => Either[Chain[SeqStringDecodeFailure], A] =
          urlForm => {
            for {
              values <- labels.zip(codecs).toList.parTraverse {
                case (name, codec) =>
                  codec
                    .asInstanceOf[SeqStringCodec[?]]
                    .decode(urlForm.get(name.toString).toList)
                    .leftMap(Chain.one)
              }
            } yield p.fromProduct(Tuple.fromArray(values.toArray[Any]))
          }

        val encode: A => UrlForm =
          a => {
            val product = a.asInstanceOf[Product]

            codecs.zipWithIndex.foldLeft(UrlForm.empty) {
              case (urlForm, (codec, idx)) =>
                codec match {
                  case c: SeqStringCodec[a] =>
                    urlForm.updateFormFields(
                      product.productElementName(idx),
                      Chain.fromSeq(
                        codec
                          .asInstanceOf[SeqStringCodec[Any]]
                          .encode(product.productElement(idx))
                      )
                    )
                }
            }
          }

        FormCodec(decode = decode, encode = encode)
    }
}
