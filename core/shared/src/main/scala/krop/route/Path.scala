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

import krop.Types
import krop.raise.Raise
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
    // The number of segments that are of type Param and hence the length of the
    // Tuple P
    val paramCount: Int,
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
    Path(segments :+ Segment.One(segment), paramCount, query, true)
  }

  /** Add a segment to this `Path`. */
  def /(segment: Segment): Path[P, Q] = {
    assertOpen()
    segment match {
      case Segment.One(_) => Path(segments :+ segment, paramCount, query, true)
      case Segment.All    => Path(segments :+ segment, paramCount, query, false)
    }
  }

  /** Add a segment that extracts a parameter to this `Path`. */
  def /[B](param: Param[B]): Path[Tuple.Append[P, B], Q] = {
    assertOpen()
    param match {
      case Param.One(_, _, _) =>
        Path(segments :+ param, paramCount + 1, query, true)
      case Param.All(_, _, _) =>
        Path(segments :+ param, paramCount + 1, query, false)
    }
  }

  def :?[B <: Tuple](query: Query[B]): Path[P, B] =
    Path(segments, paramCount, query, false)

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

  def pathAndQueryTo(pathParams: P, queryParams: Q): String = {
    val p = this.pathTo(pathParams)

    val q =
      query
        .unparse(queryParams)
        .filterNot { case (_, params) => params.isEmpty }
        .map { case (name, params) =>
          params.mkString(s"${name}=", "&name=", "")
        }
        .mkString("&")

    s"$p?$q"
  }

  /** Extract the captured parts of the URI's path. */
  def parse(
      uri: Uri
  )(using raise: Raise[ParseFailure]): Types.TupleConcat[P, Q] = {
    def loop(
        matchSegments: Vector[Segment | Param[?]],
        pathSegments: Vector[UriPath.Segment]
    ): Tuple =
      if matchSegments.isEmpty then {
        if pathSegments.isEmpty then EmptyTuple
        else Path.failure.raise(Path.failure.noMoreMatches)
      } else {
        matchSegments.head match {
          case Segment.One(value) =>
            if pathSegments.isEmpty then
              Path.failure.raise(Path.failure.noMorePathSegments)
            else {
              val decoded = pathSegments(0).decoded()

              if decoded == value then
                loop(matchSegments.tail, pathSegments.tail)
              else
                Path.failure.raise(Path.failure.segmentMismatch(decoded, value))
            }

          case Segment.All => EmptyTuple

          case Param.One(_, parse, _) =>
            if pathSegments.isEmpty then
              Path.failure.raise(Path.failure.noMorePathSegments)
            else
              parse(pathSegments(0).decoded()) match {
                case Left(err) =>
                  Path.failure.raise(Path.failure.paramMismatch(err))
                case Right(value) =>
                  value *: loop(matchSegments.tail, pathSegments.tail)
              }

          case Param.All(_, parse, _) =>
            parse(pathSegments.map(_.decoded())) match {
              case Left(err) =>
                Path.failure.raise(Path.failure.paramMismatch(err))
              case Right(value) => value *: EmptyTuple
            }
        }
      }

    val p: P = loop(segments, uri.path.segments).asInstanceOf[P]
    val q: Q = query.parse(uri.multiParams) match {
      case Left(err)    => Path.failure.raise(Path.failure.queryFailure(err))
      case Right(value) => value
    }
    val result = q match {
      case EmptyTuple => p
      case other      => p ++ other
    }

    result.asInstanceOf[Types.TupleConcat[P, Q]]
  }

  /** Convenience that is mostly used for testing */
  def parseToOption(uri: Uri): Option[Types.TupleConcat[P, Q]] =
    Raise.toOption(parse(uri))

  /** Produce a Uri that represents this Path applied to the given parameters.
    * The Uri will be empty except for the path and query components. In
    * particular, it will have no scheme or authority.
    */
  def unparse(params: Types.TupleConcat[P, Q]): Uri = {
    val ps = params.toIArray
    val (pArr, qArr) = ps.splitAt(paramCount)

    val q = Tuple.fromIArray(qArr).asInstanceOf[Q]
    val uriQuery = org.http4s.Query.fromMap(query.unparse(q))

    @tailrec
    def loop(
        idx: Int,
        segments: Vector[Segment | Param[?]],
        path: Uri.Path
    ): Uri.Path = {
      if segments.isEmpty then path
      else {
        val hd = segments.head
        val tl = segments.tail

        hd match {
          case Segment.All => path.addEndsWithSlash
          case Segment.One(value) =>
            loop(idx, tl, path.addSegment(value))
          case p: Param.All[a] =>
            path.addSegments(
              p.unparse(pArr(idx).asInstanceOf[a]).map(Uri.Path.Segment.apply)
            )
          case p: Param.One[a] =>
            loop(
              idx + 1,
              tl,
              path.addSegment(p.unparse(pArr(idx).asInstanceOf[a]))
            )
        }
      }
    }

    val uriPath = loop(0, segments, Uri.Path.Root)
    Uri(path = uriPath, query = uriQuery)
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
  final val root =
    Path[EmptyTuple, EmptyTuple](Vector.empty, 0, Query.empty, true)

  /** Create a `Path` that matches the given segment. */
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

  /** This contains detailed descriptions of why a Path can fail, and utilites
    * to construct a `ParseFailure` instances and raise them.
    */
  object failure {
    def raise(reason: ParseFailure)(using raise: Raise[ParseFailure]) =
      raise.raise(reason)

    val noMoreMatches =
      ParseFailure(
        ParseStage.Uri,
        "The URI has more segments than expected",
        """The URI this Path was matching against still contains segments. However
          |this Path does not match any more segments. To match and ignore all the
          |remaining segments use Segment.all. The match and capture all remaining
          |segments use Param.seq or another variant that captures all
          |segments.""".stripMargin
      )

    val noMorePathSegments =
      ParseFailure(
        ParseStage.Uri,
        "The URI does not contain any more segments",
        """This Path is expecting one or more segments in the URI. However the URI
          |does not contain any more segment to match against.""".stripMargin
      )

    def segmentMismatch(actual: String, expected: String) =
      ParseFailure(
        ParseStage.Uri,
        "A URI segment is not the expected segment",
        s"""This Path is expecting the segment ${expected}. However the URI
           |contained the segment ${actual} which does not match.""".stripMargin
      )

    def paramMismatch(error: ParamParseFailure) =
      ParseFailure(
        ParseStage.Uri,
        "A URI segment does not match a parameter",
        s"""This Path is expecting a segment to match the Param
           |${error.description}. However the URI contained the segment
           |${error.value} which does not match.""".stripMargin
      )

    def queryFailure(error: QueryParseFailure) =
      ParseFailure(
        ParseStage.Uri,
        "The URI's query parameters did not contain an expected value",
        s"""The URI's query parameters were not successfully parsed with the
           |following problem:
           |
           |  ${error.message}""".stripMargin
      )
  }
}
