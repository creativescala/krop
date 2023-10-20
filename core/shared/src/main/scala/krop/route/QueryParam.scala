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

import krop.route.Param.All
import krop.route.Param.One

import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** A [[package.QueryParam]] extracts values from a URI's query parameters. It
  * consists of a [[package.Param]], which does the necessary type conversion,
  * and the name under which the parameters should be found.
  *
  * There are two types of `QueryParam`:
  *
  * * required params, which fail if there are no values associated with the
  * name; and
  *
  * * optional parameters, that return `None` if there is no value for the name.
  */
enum QueryParam[A] extends Query[A] {
  case Required(name: String, param: Param[A])
  case Optional[A](name: String, param: Param[A]) extends QueryParam[Option[A]]

  /** The key within the query parameters that this `QueryParam` matches */
  def name: String

  /** Get a human-readable description of this `QueryParam`. */
  def describe: String =
    this match {
      case Required(name, param) => s"${name}=${param.describe}"
      case Optional(name, param) => s"optional(${name}=${param.describe})"
    }

  def and[B](that: QueryParam[B]): QueryBuilder[(A, B)] =
    QueryBuilder(this).and(that)

  def parse(params: Map[String, List[String]]): Try[A] =
    this.extract(params.getOrElse(name, List.empty))

  def unparse(a: A): Map[String, List[String]] =
    this match {
      case Required(name, param) =>
        param match {
          case All(_, _, unparse) => Map(name -> unparse(a).toList)
          case One(_, _, unparse) => Map(name -> List(unparse(a)))
        }

      case Optional(name, param) =>
        a match {
          case Some(a1) =>
            param match {
              case All(_, _, unparse) => Map(name -> unparse(a1).toList)
              case One(_, _, unparse) => Map(name -> List(unparse(a1)))
            }
          case None => Map.empty
        }
    }

  def imap[B](f: A => B)(g: B => A): Query[B] =
    QueryBuilder(this).imap(using
      TupleApply.tuple1Apply
    )(f)(g.andThen(a => Tuple1(a)))

  def extract(params: List[String]): Try[A] =
    this match {
      case Required(name, param) =>
        param match {
          case All(_, parse, _) => parse(params)
          case One(_, parse, _) =>
            if params.isEmpty then
              Failure(
                IllegalArgumentException(
                  "Cannot extract a required query parameter from an empty list of parameters"
                )
              )
            else parse(params.head)
        }

      case Optional(name, param) =>
        param match
          case All(_, parse, _) => parse(params).map(Some(_))
          case One(_, parse, _) =>
            if params.isEmpty then Success(None)
            else parse(params.head).map(Some(_))
    }
}
object QueryParam {
  def apply[A](name: String, param: Param[A]): QueryParam[A] =
    QueryParam.Required(name, param)
}
