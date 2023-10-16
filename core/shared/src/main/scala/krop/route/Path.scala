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
import org.http4s.Uri.{Path as UriPath}

import scala.collection.mutable
import scala.util.Failure
import scala.util.Success

/** A [[krop.route.Path]] represents a pattern to match against the path
  * component of the URI of a request.`Paths` are created starting with
  * `Path.root` and then calling the `/` method to add segments to the pattern.
  * For example
  *
  * ```
  * Path.root / "user" / "create"
  * ```
  *
  * matches a request with the path `/user/create`.
  *
  * Use a [[krop.route.Param]] to capture part of the path for later processing.
  * For example
  *
  * ```
  * Path.root / "user" / Param.int / "view"
  * ```
  *
  * matches `/user/<id>/view`, where `<id>` is an `Int`, and makes the `Int`
  * value available to the request handler.
  *
  * A `Path` will fail to match if the URI's path has more segments than the
  * `Path` matches. So `Path.root / "user" / "create"` will not match
  * `/user/create/1234`. Use `Segment.all` to match and ignore all the segments
  * to the end of the URI's path. For example
  *
  * ```
  * Path.root / "assets" / Segment.all
  * ```
  *
  * will match `/assets/example.css` and `/assets/css/example.css`.
  *
  * To capture all segments to the end of the URI's path, use an instance of
  * `Param.All` such as `Param.vector`. So
  *
  * ```
  * Path.root / "assets" / Param.vector
  * ```
  *
  * will capture the remainder of the URI's path as a `Vector[String]`.
  *
  * A `Path` that matches all segments is called a closed path. Attempting to
  * add an element to a closed path will result in an exception.
  */
final class Path[A <: Tuple] private (
    segments: Vector[Segment | Param[?]],
    // Indicates if this path can still have segments added to it. A Path that
    // matches the rest of a path is not open. Otherwise it is open.
    open: Boolean
) {

  /** Overload of `pathTo` for the case where this [[package.Path]] has no
    * parameters.
    */
  // def pathTo(using ev: EmptyTuple =:= A): String =
  //   pathTo(ev(EmptyTuple))

  /** Overload of `pathTo` for the case where this [[package.Path]] has a single
    * parameter.
    */
  // def pathTo[B](param: B)(using ev: Tuple1[B] =:= A): String =
  //   pathTo(ev(Tuple1(param)))

  /** Create a `String` that links to this path with the given parameters. For
    * example, with the path
    *
    * ```scala
    * val path = Path.root / "user" / Param.id / "edit"
    * ```
    *
    * calling
    *
    * ```scala
    * path.pathTo(1234)
    * ```
    *
    * produces the `String` `"/user/1234/edit"`.
    *
    * This version of `pathTo` takes the parameters as a tuple. There are two
    * overloads that take unwrapped parameters for the case where there are no
    * or a single parameter.
    */
  def pathTo(params: A): String = {
    val paramsArray = params.toArray

    def loop(
        idx: Int,
        segments: Vector[Segment | Param[?]],
        builder: mutable.StringBuilder
    ): String = {
      if segments.isEmpty then builder.result()
      else {
        val hd = segments.head
        val tl = segments.tail

        hd match {
          case Segment.All => builder.addOne('/').result()
          case Segment.One(value) =>
            loop(idx, tl, builder.addOne('/').append(value))
          case p: Param.All[a] =>
            builder
              .addOne('/')
              .append(p.unparse(paramsArray(idx).asInstanceOf[a]).mkString("/"))
              .result()
          case p: Param.One[a] =>
            loop(
              idx + 1,
              tl,
              builder
                .addOne('/')
                .append(p.unparse(paramsArray(idx).asInstanceOf[a]))
            )
        }
      }
    }

    loop(0, segments, mutable.StringBuilder())
  }

  /** Optionally extract the captured parts of the URI's path. */
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
          case Segment.One(value) =>
            if !pathSegments.isEmpty && pathSegments(0).decoded() == value then
              loop(matchSegments.tail, pathSegments.tail)
            else None

          case Segment.All =>
            Some(EmptyTuple)

          case Param.One(_, parse, _) =>
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

          case Param.All(_, parse, _) =>
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
    Path(segments :+ Segment.One(segment), true)
  }

  def /(segment: Segment): Path[A] = {
    assertOpen()
    segment match {
      case Segment.One(_) => Path(segments :+ segment, true)
      case Segment.All    => Path(segments :+ segment, false)
    }
  }

  def /[B](param: Param[B]): Path[Tuple.Append[A, B]] = {
    assertOpen()
    param match {
      case Param.One(_, _, _) => Path(segments :+ param, true)
      case Param.All(_, _, _) => Path(segments :+ param, false)
    }
  }

  /** Produces a human-readable representation of this Path. The toString method
    * is used to output the usual programmatic representation.
    */
  def describe: String =
    segments
      .map {
        case Segment.One(v)     => v
        case Segment.All        => "rest*"
        case Param.One(n, _, _) => n
        case Param.All(n, _, _) => s"$n*"
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
