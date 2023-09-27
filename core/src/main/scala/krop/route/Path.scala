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

import org.http4s.Uri.{Path as UriPath}

import scala.util.Failure
import scala.util.Success

final class Path[A <: Tuple] private (segments: Vector[String | Param[?]]) {
  def extract(path: UriPath): Option[A] = {
    def loop(
        matchSegments: Vector[String | Param[?]],
        pathSegments: Vector[UriPath.Segment]
    ): Option[Tuple] =
      if matchSegments.isEmpty then {
        if pathSegments.isEmpty then Some(EmptyTuple)
        else None
      } else {
        matchSegments.head match {
          case s: String =>
            if !pathSegments.isEmpty && pathSegments(0).decoded() == s then
              loop(matchSegments.tail, pathSegments.tail)
            else None

          case Param(_, decoder, _) =>
            if !pathSegments.isEmpty then {
              val raw = pathSegments(0).decoded()
              val attempt = decoder(raw)
              attempt match {
                case Failure(_) => None
                case Success(value) =>
                  loop(matchSegments.tail, pathSegments.tail) match {
                    case None       => None
                    case Some(tail) => Some(value *: tail)
                  }
              }
            } else None
        }
      }

    loop(segments, path.segments).asInstanceOf[Option[A]]
  }

  def /(segment: String): Path[A] =
    Path(segments :+ segment)

  def /[B](param: Param[B]): Path[Tuple.Append[A, B]] =
    Path(segments :+ param)

  /** Produces a human-readable representation of this Path. The toString method
    * is used to output the usual programmatic representation.
    */
  def describe: String =
    segments
      .map {
        case s: String      => s
        case Param(n, _, _) => n
      }
      .mkString("/", "/", "")
}
object Path {
  val root = Path[EmptyTuple](Vector.empty)
}
