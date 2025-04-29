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
  import QueryParseFailure.*

  /** Get a human-readable description of this `QueryParam`. */
  def describe: String = ???

  def decode(params: Map[String, List[String]]): Either[QueryParseFailure, A] =
    this match {
      case One(name, codec) =>
        params.get(name) match {
          case Some(values) =>
            values.headOption match {
              case None => NoValuesForName(name).asLeft
              case Some(value) =>
                codec.decode(value) match {
                  case Right(value) => Right(value)
                  case Left(error) =>
                    ValueParsingFailed(name, value, error.description).asLeft
                }
            }

          case None => NoParameterWithName(name).asLeft
        }

      case All(name, codec) =>
        params.get(name) match {
          case Some(values) =>
            codec.decode(values) match {
              case Right(value) => Right(value)
              case Left(error) =>
                ValueParsingFailed(
                  name,
                  values.toString,
                  error.description
                ).asLeft
            }

          case None => NoParameterWithName(name).asLeft
        }

      case Optional(name, codec) =>
        params.get(name) match {
          case Some(values) =>
            if values.isEmpty then None.asRight
            else {
              val hd = values.head
              codec
                .decode(hd)
                .map(Some(_))
                .leftMap(error =>
                  ValueParsingFailed(name, hd, error.description)
                )
            }

          case None => None.asRight
        }

      case Everything => params.asRight
    }

  def encode(a: A): Option[(String, Seq[String])] =
    this match {
      case One(name, codec) =>
        Some(name -> List(codec.encode(a)))

      case All(name, codec) =>
        Some(name -> codec.encode(a))

      case Optional(name, codec) =>
        a match {
          case Some(a1) => Some(name -> List(codec.encode(a1)))
          case None     => None
        }

      case Everything => None
    }

  case One(name: String, codec: StringCodec[A])
  case All(name: String, codec: SeqStringCodec[A])
  case Optional(name: String, codec: StringCodec[A])
      extends QueryParam[Option[A]]
  case Everything extends QueryParam[Map[String, List[String]]]
}
object QueryParam {

  /** Construct a [[QueryParam]] that decodes the first value from any query
    * parameters with the given name.
    */
  def one[A](name: String)(using codec: StringCodec[A]): QueryParam[A] =
    QueryParam.One(name, codec)

  /** Construct a [[QueryParam]] that decodes all the values from any query
    * parameters with the given name.
    */
  def all[A](name: String)(using codec: SeqStringCodec[A]): QueryParam[A] =
    QueryParam.All(name, codec)

  /** Construct a [[QueryParam]] that decodes the first value from any query
    * parameters with the given name, and returns None if there are no values.
    */
  def optional[A](name: String)(using
      codec: StringCodec[A]
  ): QueryParam[Option[A]] =
    QueryParam.Optional(name, codec)

  def int(name: String)(using StringCodec[Int]): QueryParam[Int] =
    one[Int](name)

  def string(name: String)(using StringCodec[String]): QueryParam[String] =
    one[String](name)

  /** A QueryParam that returns all the query parameters unchanged. */
  val everything: QueryParam[Map[String, List[String]]] =
    QueryParam.Everything

  def apply[A](name: String, codec: StringCodec[A]): QueryParam[A] =
    QueryParam.One(name, codec)
}
