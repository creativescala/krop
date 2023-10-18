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
import krop.route.Param.All
import krop.route.Param.One

import scala.util.Try

final class QueryBuilder[A <: Tuple] private[route] (
    params: Vector[QueryParam[?]]
) {
  def and[B](that: QueryParam[B]): QueryBuilder[Tuple.Append[A, B]] =
    new QueryBuilder(params :+ that)

  def imap[B](using ta: TupleApply[A, B])(f: ta.Fun)(g: B => A): Query[B] =
    new Query[B] {
      def describe: String = params.map(_.describe).mkString("&")

      def parse(queryParams: Map[String, List[String]]): Try[B] =
        params
          .traverse(p => p.extract(queryParams.getOrElse(p.name, List.empty)))
          .map(v => Tuple.fromArray(v.toArray))
          .asInstanceOf[Try[A]]
          .map(a => ta.tuple(f)(a))

      def unparse(b: B): Map[String, List[String]] = {
        val a = g(b).toIArray

        def optUnparse[A](
            param: QueryParam.Optional[A],
            elt: Object
        ): (String, List[String]) =
          param.param match {
            case All(_, _, unparse) =>
              param.name -> unparse(elt.asInstanceOf[A]).toList
            case One(_, _, unparse) =>
              param.name -> List(unparse(elt.asInstanceOf[A]))
          }

        def requiredUnparse[A](
            param: QueryParam.Required[A],
            elt: Object
        ): (String, List[String]) =
          param.param match {
            case All(_, _, unparse) =>
              param.name -> unparse(elt.asInstanceOf[A]).toList
            case One(_, _, unparse) =>
              param.name -> List(unparse(elt.asInstanceOf[A]))
          }

        def unparse[A](
            param: QueryParam[A],
            elt: Object
        ): (String, List[String]) =
          param match {
            case p: QueryParam.Required[A] => requiredUnparse(p, elt)
            case p: QueryParam.Optional[a] => optUnparse(p, elt)
          }

        def loop(
            idx: Int,
            accum: List[(String, List[String])]
        ): Map[String, List[String]] = {
          if idx == params.size then accum.toMap
          else {
            val param = params(idx)
            val elt = a(idx)

            loop(idx + 1, unparse(param, elt) :: accum)
          }
        }

        loop(0, List.empty)
      }
    }
}
