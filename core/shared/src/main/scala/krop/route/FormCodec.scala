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
import cats.effect.IO
import krop.route.Param.All
import krop.route.Param.One
import org.http4s.DecodeFailure
import org.http4s.UrlForm

import scala.compiletime.*
import scala.deriving.*
import scala.quoted.*

final case class FormCodec[A](
    decode: UrlForm => Either[DecodeFailure, A],
    encode: A => UrlForm
)

object FormCodec {
  inline def derived[A]: FormCodec[A] = ${ derivedMacro[A] }

  def derivedMacro[A: Type](using Quotes): Expr[FormCodec[A]] = {
    val ev: Expr[Mirror.Of[A]] = Expr.summon[Mirror.Of[A]].get

    ev match
      case '{
            $m: Mirror.ProductOf[A] { type MirroredElemTypes = elementTypes }
          } =>
        val elemInstances = summonInstances[A, elementTypes]
        def encodeProductBody(v: Expr[Product])(using Quotes): Expr[UrlForm] = {
          if elemInstances.isEmpty then '{ UrlForm.empty }
          else {
            val elts =
              elemInstances.zipWithIndex
                .map { case ('{ $elem: Param[t] }, index) =>
                  val indexExpr = Expr(index)
                  val e = '{ $v.productElement($indexExpr).asInstanceOf[t] }
                  val name = '{ $v.productElementName($indexExpr) }
                  '{
                    $elem match {
                      case All(_, _, unparse) =>
                        Map($name -> Chain.fromSeq(unparse($e)))
                      case One(_, _, unparse) =>
                        Map($name -> Chain(unparse($e)))
                    }
                  }
                }
                .reduce((acc, elem) => '{ $acc ++ $elem })

            '{ UrlForm($elts) }
          }
        }
        '{
          formCodecProduct((v: A) =>
            ${ encodeProductBody('v.asExprOf[Product]) }
          )
        }

      case '{ $m: Mirror.SumOf[A] { type MirroredElemTypes = elementTypes } } =>
        '{
          error(
            "Generic derivation of FormCodec instances is not supported for sum types."
          )
        }
  }

  private def formCodecProduct[A](encode: A => UrlForm): FormCodec[A] =
    FormCodec(decode = _ => ???, encode = encode)

  private def summonInstances[A: Type, Elems: Type](using
      Quotes
  ): List[Expr[Param[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] =>
        '{ summonInline[Param[elem]] } :: summonInstances[A, elems]
      case '[EmptyTuple] => Nil
}
