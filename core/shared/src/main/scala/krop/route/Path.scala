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
import krop.route
import krop.route.Param.All
import krop.route.Param.One
import org.http4s.Uri
import org.http4s.Uri.Path as UriPath

import scala.annotation.tailrec
import scala.collection.mutable
import scala.compiletime.constValue

/** A [[krop.route.Path]] represents a pattern to match against the path
  * component of the URI of a request.`Paths` are created by calling the `/`
  * method to add segments to the pattern. For example
  *
  * ```
  * Path / "user" / "create"
  * ```
  *
  * matches a request with the path `/user/create`.
  *
  * Use a [[krop.route.Param]] to capture part of the path for later processing.
  * For example
  *
  * ```
  * Path / "user" / Param.int / "view"
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
  * Path / "assets" / Segment.all
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
final class Path[P <: Tuple, Q <: Tuple] private (
    val segments: Vector[Segment | Param[?]],
    val query: Query[Q],
    // Indicates if this path can still have segments added to it. A Path that
    // matches the rest of a path is not open. Otherwise it is open.
    open: Boolean
) {
  //
  // Combinators ---------------------------------------------------------------
  //

  /** Add a segment to this `Path`. */
  def /(segment: String): Path[P, Q] = {
    assertOpen()
    Path(segments :+ Segment.One(segment), query, true)
  }

  /** Add a segment to this `Path`. */
  def /(segment: Segment): Path[P, Q] = {
    assertOpen()
    segment match {
      case Segment.One(_) => Path(segments :+ segment, query, true)
      case Segment.All    => Path(segments :+ segment, query, false)
    }
  }

  /** Add a segment that extracts a parameter to this `Path`. */
  def /[B](param: Param[B]): Path[Tuple.Append[P, B], Q] = {
    assertOpen()
    param match {
      case Param.One(_, _, _) => Path(segments :+ param, query, true)
      case Param.All(_, _, _) => Path(segments :+ param, query, false)
    }
  }

  def :?[B <: Tuple](query: Query[B]): Path[P, B] =
    Path(segments, query, false)

  private def assertOpen(): Boolean =
    if open then true
    else
      throw new IllegalStateException(
        s"""Cannot add a segment or parameter to a closed path.
           |
           |  A path is closed when it has a segment or parameter that matches all remaining elements.
           |  A closed path cannot have additional segments of parameters added to it.""".stripMargin
      )

  //
  // Interpreters --------------------------------------------------------------
  //

  /** Overload of `pathTo` for the case where the path has no parameters.
    */
  def pathTo(using ev: EmptyTuple =:= P): String =
    pathTo(ev(EmptyTuple))

  /** Overload of `pathTo` for the case where the path has a single parameter.
    */
  def pathTo[B](param: B)(using ev: Tuple1[B] =:= P): String =
    pathTo(ev(Tuple1(param)))

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
    */
  def pathTo(params: P): String = {
    val paramsArray = params.toArray

    @tailrec
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

    loop(0, segments, StringBuilder())
  }

  def pathAndQueryTo(params: P, queryParam: Q): String = {
    val qParams =
      query
        .unparse(queryParam)
        .filterNot { case (_, params) => params.isEmpty }
        .map { case (name, params) =>
          params.mkString(s"${name}=", "&name=", "")
        }
        .mkString("&")

    s"pathTo(params)?$qParams"
  }

  /** Optionally extract the captured parts of the URI's path. */
  def extract(uri: Uri): Option[Tuple.Concat[P, Q]] = {
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
            if pathSegments.nonEmpty && pathSegments(0).decoded() == value then
              loop(matchSegments.tail, pathSegments.tail)
            else None

          case Segment.All => Some(EmptyTuple)

          case Param.One(_, parse, _) =>
            if pathSegments.isEmpty then None
            else
              parse(pathSegments(0).decoded()) match {
                case Left(_) => None
                case Right(value) =>
                  loop(matchSegments.tail, pathSegments.tail) match {
                    case None       => None
                    case Some(tail) => Some(value *: tail)
                  }
              }

          case Param.All(_, parse, _) =>
            parse(pathSegments.map(_.decoded())) match {
              case Left(_)      => None
              case Right(value) => Some(value *: EmptyTuple)
            }
        }
      }

    val result =
      for {
        p <- loop(segments, uri.path.segments).asInstanceOf[Option[P]]
        q <- query.parse(uri.multiParams).toOption
      } yield q match {
        case EmptyTuple => p
        case other      => p :* other
      }

    result.asInstanceOf[Option[Tuple.Concat[P, Q]]]
  }

  /** Produces a human-readable representation of this Path. The toString method
    * is used to output the usual programmatic representation.
    */
  def describe: String = {
    val p = segments
      .map {
        case s: Segment  => s.describe
        case p: Param[?] => p.describe
      }
      .mkString("/", "/", "")

    val q = query.describe

    if q.isEmpty then p else s"$p?$q"
  }
}
object Path {

  /** The `Path` representing the root. You can start constructing paths using
    * `Path.root` but it is more idiomatic to call one of the `/` method
    * directly on the `Path` companion object.
    */
  final val root = Path[EmptyTuple, EmptyTuple](Vector.empty, Query.empty, true)

  /** Create a `Path` that matches theg given segment. */
  def /(segment: String): Path[EmptyTuple, EmptyTuple] =
    root / segment

  /** Create a `Path` that matches the given segment. */
  def /(segment: Segment): Path[EmptyTuple, EmptyTuple] =
    root / segment

  /** Create a `Path` that matches the given segment and extracts it as a
    * parameter.
    */
  def /[A](param: Param[A]): Path[Tuple1[A], EmptyTuple] =
    root / param
}
