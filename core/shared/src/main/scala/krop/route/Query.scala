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

import cats.syntax.all.*

final case class Query[A <: Tuple](segments: Vector[QueryParam[?]]) {
  //
  // Combinators ---------------------------------------------------------------
  //

  def ++[B <: Tuple](that: Query[B]): Query[Tuple.Concat[A, B]] =
    Query(this.segments ++ that.segments)

  def and[B](param: QueryParam[B]): Query[Tuple.Append[A, B]] =
    Query(segments :+ param)

  def and[B](name: String)(using StringCodec[B]): Query[Tuple.Append[A, B]] =
    this.and(QueryParam.one(name))

  //
  // Interpreters --------------------------------------------------------------
  //

  def decode(params: Map[String, List[String]]): Either[QueryParseFailure, A] =
    segments
      .traverse(s => s.decode(params))
      .map(v => Tuple.fromArray(v.toArray).asInstanceOf[A])

  def encode(a: A): Map[String, Seq[String]] = {
    val aArray = a.toArray

    def loop(
        idx: Int,
        segments: Vector[QueryParam[?]],
        accum: Map[String, Seq[String]]
    ): Map[String, Seq[String]] =
      if segments.isEmpty then accum
      else {
        val hd = segments.head
        val tl = segments.tail

        hd match {
          case q: QueryParam.One[a] =>
            loop(
              idx + 1,
              tl,
              q.encode(aArray(idx).asInstanceOf[a])
                .fold(accum)(p => accum + p)
            )

          case q: QueryParam.All[a] =>
            loop(
              idx + 1,
              tl,
              q.encode(aArray(idx).asInstanceOf[a])
                .fold(accum)(p => accum + p)
            )

          case q: QueryParam.Optional[a] =>
            loop(
              idx + 1,
              tl,
              q.encode(aArray(idx).asInstanceOf[Option[a]])
                .fold(accum)(p => accum + p)
            )

          case QueryParam.Everything => loop(idx + 1, tl, accum)
        }
      }

    loop(0, segments, Map.empty)
  }

  def describe: String =
    segments.map(_.describe).mkString("&")
}
object Query {
  val empty: Query[EmptyTuple] =
    Query(Vector.empty)

  def apply[A](param: QueryParam[A]): Query[Tuple1[A]] =
    Query(Vector(param))

  def apply[A](name: String)(using StringCodec[A]): Query[Tuple1[A]] =
    Query(Vector(QueryParam.one(name)))

  def all[A](name: String)(using SeqStringCodec[A]): Query[Tuple1[A]] =
    Query(Vector(QueryParam.all(name)))

  def optional[A](name: String)(using
      StringCodec[A]
  ): Query[Tuple1[Option[A]]] =
    Query(Vector(QueryParam.optional(name)))

  val everything: Query[Tuple1[Map[String, List[String]]]] =
    Query(QueryParam.everything)
}
