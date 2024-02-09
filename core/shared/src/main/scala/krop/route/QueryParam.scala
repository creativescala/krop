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

import krop.route.Param.One

import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** A [[package.QueryParam]] extracts values from a URI's query parameters. It
  * consists of a [[package.Param]], which does the necessary type conversion,
  * and the name under which the parameters should be found.
  *
  * There are three types of `QueryParam`:
  *
  * * required params, which fail if there are no values associated with the
  * name;
  *
  * * optional parameters, that return `None` if there is no value for the name;
  * and
  *
  * * the `QueryParam` that returns all the query parameters.
  */
enum QueryParam[A] {
  import QueryParseException.*

  /** Get a human-readable description of this `QueryParam`. */
  def describe: String =
    this match {
      case Required(name, param) => s"${name}=${param.describe}"
      case Optional(name, param) => s"optional(${name}=${param.describe})"
      case All                   => "all"
    }

  def parse(params: Map[String, List[String]]): Try[A] =
    this match {
      case Required(name, param) =>
        params.get(name) match {
          case Some(values) =>
            param match {
              case Param.All(_, parse, _) => parse(values)
              case Param.One(_, parse, _) =>
                if values.isEmpty then Failure(NoValuesForName(name))
                else {
                  val hd = values.head
                  parse(hd).recoverWith(_ =>
                    Failure(
                      QueryParseException.ValueParsingFailed(name, hd, param)
                    )
                  )
                }
            }
          case None => Failure(NoParameterWithName(name))
        }

      case Optional(name, param) =>
        params.get(name) match {
          case Some(values) =>
            param match {
              case Param.All(_, parse, _) => parse(values).map(Some(_))
              case Param.One(_, parse, _) =>
                if values.isEmpty then Success(None)
                else {
                  val hd = values.head
                  parse(hd)
                    .map(Some(_))
                    .recoverWith(_ =>
                      Failure(
                        QueryParseException.ValueParsingFailed(name, hd, param)
                      )
                    )
                }
            }

          case None => Success(None)
        }

      case All => Success(params)
    }

  def unparse(a: A): Option[(String, List[String])] =
    this match {
      case Required(name, param) =>
        param match {
          case Param.All(_, _, unparse) => Some(name -> unparse(a).toList)
          case Param.One(_, _, unparse) => Some(name -> List(unparse(a)))
        }

      case Optional(name, param) =>
        a match {
          case Some(a1) =>
            param match {
              case Param.All(_, _, unparse) => Some(name -> unparse(a1).toList)
              case Param.One(_, _, unparse) => Some(name -> List(unparse(a1)))
            }
          case None => None
        }

      case All => None
    }

  case Required(name: String, param: Param[A])
  case Optional[A](name: String, param: Param[A]) extends QueryParam[Option[A]]
  case All extends QueryParam[Map[String, List[String]]]
}
object QueryParam {
  def apply[A](name: String, param: Param[A]): QueryParam[A] =
    QueryParam.Required(name, param)

  def optional[A](name: String, param: Param[A]): QueryParam[Option[A]] =
    QueryParam.Optional(name, param)

  val all = QueryParam.All
}
