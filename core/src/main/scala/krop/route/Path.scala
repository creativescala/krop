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

/** A [[KRop.route.Path]] represents a pattern to match against the path
  * component of the URI part of a request.
  *
  * Paths are created starting with `Path.root` and then calling the `/` method
  * to add segments to the path.
  *
  * ```
  * // Matches /user/create
  * Path.root / "user" / "create"
  * ```
  *
  * If you want to capture part of the path as a parameter for later processing,
  * use a [[krop.route.Param]].
  *
  * ```
  * // Matches /user/<id>/view, where <id> is an Int
  * Path.root / "user" / Param.int / "view"
  * ```
  *
  * A Path only matches the path component of the URI if they have exactly the
  * same number of segments. So `Path.root / "user" / "create"` will not match
  * "/user/create/1234".
  */
final class Path[A <: Tuple] private (
    segments: Vector[Segment | Param[?]],
    // Indicates if this path can still have segments added to it. A Path that
    // matches the rest of a path is not open. Otherwise it is open.
    open: Boolean
) {
  def extract(path: UriPath): Option[A] = {
    def loop(
        matchSegments: Vector[Segment | Param[?]],
        pathSegments: Vector[UriPath.Segment]
    ): Option[Tuple] =
      if matchSegments.isEmpty then {
        if pathSegments.isEmpty then Some(EmptyTuple)
        else None
      } else {
        matchSegments.head match {
          case Segment.Part(value) =>
            if !pathSegments.isEmpty && pathSegments(0).decoded() == value then
              loop(matchSegments.tail, pathSegments.tail)
            else None

          case Segment.Rest =>
            Some(EmptyTuple)

          case Param.Part(_, parse, _) =>
            if !pathSegments.isEmpty then {
              val raw = pathSegments(0).decoded()
              val attempt = parse(raw)
              attempt match {
                case Failure(_) => None
                case Success(value) =>
                  loop(matchSegments.tail, pathSegments.tail) match {
                    case None       => None
                    case Some(tail) => Some(value *: tail)
                  }
              }
            } else None

          case Param.Rest(_, parse, _) =>
            parse(pathSegments.map(_.decoded())) match {
              case Failure(_)     => None
              case Success(value) => Some(value *: EmptyTuple)
            }
        }
      }

    loop(segments, path.segments).asInstanceOf[Option[A]]
  }

  def /(segment: String): Path[A] = {
    assertOpen()
    Path(segments :+ Segment.Part(segment), true)
  }

  def /(segment: Segment): Path[A] = {
    assertOpen()
    segment match {
      case Segment.Part(_) => Path(segments :+ segment, true)
      case Segment.Rest    => Path(segments :+ segment, false)
    }
  }

  def /[B](param: Param[B]): Path[Tuple.Append[A, B]] = {
    assertOpen()
    param match {
      case Param.Part(_, _, _) => Path(segments :+ param, true)
      case Param.Rest(_, _, _) => Path(segments :+ param, false)
    }
  }

  /** Produces a human-readable representation of this Path. The toString method
    * is used to output the usual programmatic representation.
    */
  def describe: String =
    segments
      .map {
        case Segment.Part(v)     => v
        case Segment.Rest        => "rest*"
        case Param.Part(n, _, _) => n
        case Param.Rest(n, _, _) => s"$n*"
      }
      .mkString("/", "/", "")

  private def assertOpen(): Boolean =
    if open then true
    else
      throw new IllegalStateException(
        s"""Cannot add a segment or parameter to a closed path.
           |
           |  A path is closed when it has a segment or parameter that matches all remaining elements.
           |  A closed path cannot have additional segments of parameters added to it.""".stripMargin
      )
}
object Path {
  val root = Path[EmptyTuple](Vector.empty, true)
}
