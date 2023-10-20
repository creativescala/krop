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

final class QueryBuilder[A <: Tuple] private[route] (
    params: Tuple.Map[A, QueryParam]
) {
  def and[B](that: QueryParam[B]): QueryBuilder[Tuple.Append[A, B]] =
    new QueryBuilder(
      (params :* that).asInstanceOf[Tuple.Map[Tuple.Append[A, B], QueryParam]]
    )

  def imap[B](using ta: TupleApply[A, B])(f: ta.Fun)(g: B => A): Query[B] =
    new Query[B] {
      val paramsList: List[QueryParam[?]] =
        params.toList.asInstanceOf[List[QueryParam[?]]]

      def describe: String =
        paramsList
          .map(q => q.describe)
          .mkString("&")

      def parse(queryParams: Map[String, List[String]]): Try[B] =
        paramsList
          .traverse(p => p.extract(queryParams.getOrElse(p.name, List.empty)))
          .map(v => Tuple.fromArray(v.toArray))
          .asInstanceOf[Try[A]]
          .map(a => ta.tuple(f)(a))

      def unparse(b: B): Map[String, List[String]] = {
        val a = g(b).toIArray

        def unparse[A](
            param: QueryParam[A],
            elt: Object
        ): Map[String, List[String]] =
          param match {
            case p: QueryParam.Required[A] => p.unparse(elt.asInstanceOf[A])
            case p: QueryParam.Optional[a] => p.unparse(elt.asInstanceOf[A])
          }

        def loop(
            idx: Int,
            params: List[QueryParam[?]],
            accum: Map[String, List[String]]
        ): Map[String, List[String]] = {
          params match
            case head :: next =>
              val elt = a(idx)
              loop(idx + 1, next, unparse(head, elt) ++ accum)

            case Nil => accum.toMap
        }

        loop(0, paramsList, Map.empty)
      }
    }
}
object QueryBuilder {
  def apply[A](param: QueryParam[A]): QueryBuilder[Tuple1[A]] =
    new QueryBuilder[Tuple1[A]](Tuple1(param))
}
