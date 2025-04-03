package krop.route

import org.http4s.UrlForm
import org.http4s.DecodeFailure

import scala.compiletime.*
import scala.deriving.*
import scala.quoted.*

import cats.effect.IO

final case class FormCodec[A](
    decode: UrlForm => IO[Either[DecodeFailure, A]],
    encode: A => UrlForm
)

object FormCodec {
  private def summonInstances[A: Type, Elems: Type](using
      Quotes
  ): List[Expr[FormCodec[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] =>
        deriveOrSummon[A, elem] :: summonInstances[A, elems]
      case '[EmptyTuple] => Nil

  private def deriveOrSummon[A: Type, Elem: Type](using
      Quotes
  ): Expr[FormCodec[Elem]] =
    Type.of[Elem] match
      case '[A] => deriveRec[A, Elem]
      case _    => '{ summonInline[FormCodec[Elem]] }

  def deriveRec[A: Type, Elem: Type](using Quotes): Expr[FormCodec[Elem]] =
    Type.of[A] match
      case '[Elem] =>
        '{
          error(
            "Detected infinite recursion in derivation of FormCodec instance."
          )
        }
      case _ => derivedMacro[Elem] // recursive derivation

  inline def derived[A]: FormCodec[A] = ${ derivedMacro[A] }

  def derivedMacro[A: Type](using Quotes): Expr[FormCodec[A]] = {
    val ev: Expr[Mirror.Of[A]] = Expr.summon[Mirror.Of[A]].get

    ev match
      case '{
            $m: Mirror.ProductOf[A] { type MirroredElemTypes = elementTypes }
          } =>
        val elemInstances = summonInstances[A, elementTypes]
        def eqProductBody(x: Expr[Product], y: Expr[Product])(using
            Quotes
        ): Expr[Boolean] = {
          if elemInstances.isEmpty then Expr(true)
          else
            elemInstances.zipWithIndex
              .map { case ('{ $elem: FormCodec[t] }, index) =>
                val indexExpr = Expr(index)
                val e1 = '{ $x.productElement($indexExpr).asInstanceOf[t] }
                val e2 = '{ $y.productElement($indexExpr).asInstanceOf[t] }
                '{ $elem.eqv($e1, $e2) }
              }
              .reduce((acc, elem) => '{ $acc && $elem })
          end if
        }
        '{
          eqProduct((x: T, y: T) =>
            ${ eqProductBody('x.asExprOf[Product], 'y.asExprOf[Product]) }
          )
        }

      case '{ $m: Mirror.SumOf[A] { type MirroredElemTypes = elementTypes } } =>
        val elemInstances = summonInstances[A, elementTypes]
        val elements = Expr.ofList(elemInstances)

        def eqSumBody(x: Expr[T], y: Expr[T])(using Quotes): Expr[Boolean] =
          val ordx = '{ $m.ordinal($x) }
          val ordy = '{ $m.ordinal($y) }
          '{
            $ordx == $ordy && $elements($ordx)
              .asInstanceOf[FormCodec[Any]]
              .eqv($x, $y)
          }

        '{ eqSum((x: T, y: T) => ${ eqSumBody('x, 'y) }) }
  }
}
