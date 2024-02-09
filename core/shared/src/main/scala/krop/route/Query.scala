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

import scala.util.Try

final case class Query[A <: Tuple](segments: Vector[QueryParam[?]]) {
  //
  // Combinators ---------------------------------------------------------------
  //

  def ++[B <: Tuple](that: Query[B]): Query[Tuple.Concat[A, B]] =
    Query(this.segments ++ that.segments)

  def and[B](param: QueryParam[B]): Query[Tuple.Append[A, B]] =
    Query(segments :+ param)

  def and[B](name: String, param: Param[B]): Query[Tuple.Append[A, B]] =
    this.and(QueryParam(name, param))

  //
  // Interpreters --------------------------------------------------------------
  //

  def parse(params: Map[String, List[String]]): Try[A] =
    segments
      .traverse(s => s.parse(params))
      .map(v => Tuple.fromArray(v.toArray).asInstanceOf[A])

  def unparse(a: A): Map[String, List[String]] = {
    val aArray = a.toArray

    def loop(
        idx: Int,
        segments: Vector[QueryParam[?]],
        accum: Map[String, List[String]]
    ): Map[String, List[String]] =
      if segments.isEmpty then accum
      else {
        val hd = segments.head
        val tl = segments.tail

        hd match {
          case q: QueryParam.Required[a] =>
            loop(
              idx + 1,
              tl,
              q.unparse(aArray(idx).asInstanceOf[a])
                .fold(accum)(p => accum + p)
            )
          case q: QueryParam.Optional[a] =>
            loop(
              idx + 1,
              tl,
              q.unparse(aArray(idx).asInstanceOf[Option[a]])
                .fold(accum)(p => accum + p)
            )
          case QueryParam.All => loop(idx + 1, tl, accum)
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

  def apply[A](name: String, param: Param[A]): Query[Tuple1[A]] =
    Query(Vector(QueryParam(name, param)))
}
