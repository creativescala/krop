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
import org.http4s.Uri
import org.http4s.Uri.Path as UriPath

import scala.annotation.tailrec
import scala.collection.mutable

/** A [[krop.route.Path]] represents a pattern to match against the path
  * component of the URI of a request. `Paths` are created by calling the `/`
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
  * `/user/create/1234`. Use [[Segments]] to match and ignore all the segments
  * to the end of the URI's path. For example
  *
  * ```
  * Path / "assets" / Segments
  * ```
  *
  * will match `/assets/example.css` and `/assets/css/example.css`.
  *
  * To capture all segments to the end of the URI's path, use [[Params]]. So
  *
  * ```
  * Path.root / "assets" / Params.seq
  * ```
  *
  * will capture the remainder of the URI's path as a `Seq[String]`.
  *
  * A `Path` that ends with [[Segments]], [[Params]], or a query (`:?`) is
  * called a closed path. The type parameter `S` tracks this at compile time:
  * [[Path.Open]] paths can have further segments added; [[Path.Closed]] paths
  * cannot.
  */
final class Path[P <: Tuple, Q <: Tuple, S] private (
    val segments: Vector[Segment | Segments.type | Param[?] | Params[?]],
    // The number of segments that are of type Param and hence the length of the
    // Tuple P
    val paramCount: Int,
    val query: Query[Q]
) {

  //
  // Interpreters --------------------------------------------------------------
  //

  /** Create a `String` that links to this path with the given parameters. For
    * example, with the path
    *
    * ```scala
    * val path = Path / "user" / Param.int / "edit"
    * ```
    *
    * calling
    *
    * ```scala
    * path.pathTo(Tuple1(1234))
    * ```
    *
    * produces the `String` `"/user/1234/edit"`.
    */
  def pathTo(params: P): String = {
    val paramsArray = params.toArray

    @tailrec
    def loop(
        idx: Int,
        segments: Vector[Segment | Segments.type | Param[?] | Params[?]],
        builder: mutable.StringBuilder
    ): String = {
      if segments.isEmpty then builder.result()
      else {
        val hd = segments.head
        val tl = segments.tail

        hd match {
          case Segments => builder.addOne('/').result()
          case s: Segment =>
            loop(idx, tl, builder.addOne('/').append(s.value))
          case p: Params[a] =>
            builder
              .addOne('/')
              .append(p.encode(paramsArray(idx).asInstanceOf[a]).mkString("/"))
              .result()
          case p: Param[a] =>
            loop(
              idx + 1,
              tl,
              builder
                .addOne('/')
                .append(p.encode(paramsArray(idx).asInstanceOf[a]))
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
        .encode(queryParams)
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
        matchSegments: Vector[Segment | Segments.type | Param[?] | Params[?]],
        pathSegments: Vector[UriPath.Segment]
    ): Tuple =
      if matchSegments.isEmpty then {
        if pathSegments.isEmpty then EmptyTuple
        else Path.failure.raise(Path.failure.noMoreMatches)
      } else {
        matchSegments.head match {
          case s: Segment =>
            if pathSegments.isEmpty then
              Path.failure.raise(Path.failure.noMorePathSegments)
            else {
              val decoded = pathSegments(0).decoded()

              if decoded == s.value then
                loop(matchSegments.tail, pathSegments.tail)
              else
                Path.failure.raise(
                  Path.failure.segmentMismatch(decoded, s.value)
                )
            }

          case Segments => EmptyTuple

          case p: Param[a] =>
            if pathSegments.isEmpty then
              Path.failure.raise(Path.failure.noMorePathSegments)
            else
              p.decode(pathSegments(0).decoded()) match {
                case Left(err) =>
                  Path.failure.raise(Path.failure.paramMismatch(err))
                case Right(value) =>
                  value *: loop(matchSegments.tail, pathSegments.tail)
              }

          case p: Params[a] =>
            p.decode(pathSegments.map(_.decoded())) match {
              case Left(err) =>
                Path.failure.raise(Path.failure.paramMismatch(err))
              case Right(value) => value *: EmptyTuple
            }
        }
      }

    val p: P = loop(segments, uri.path.segments).asInstanceOf[P]
    val q: Q = query.decode(uri.multiParams) match {
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
    val uriQuery = org.http4s.Query.fromMap(query.encode(q))

    @tailrec
    def loop(
        idx: Int,
        segments: Vector[Segment | Segments.type | Param[?] | Params[?]],
        path: Uri.Path
    ): Uri.Path = {
      if segments.isEmpty then path
      else {
        val hd = segments.head
        val tl = segments.tail

        hd match {
          case Segments => path.addEndsWithSlash
          case s: Segment =>
            loop(idx, tl, path.addSegment(s.value))
          case p: Params[a] =>
            path.addSegments(
              p.encode(pArr(idx).asInstanceOf[a]).map(Uri.Path.Segment.apply)
            )
          case p: Param[a] =>
            loop(
              idx + 1,
              tl,
              path.addSegment(p.encode(pArr(idx).asInstanceOf[a]))
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
        case s: Segment   => s.describe
        case Segments     => Segments.describe
        case p: Param[?]  => p.name
        case p: Params[?] => p.name
      }
      .mkString("/", "/", "")

    val q = query.describe

    if q.isEmpty then p else s"$p?$q"
  }
}
object Path {

  /** Phantom type indicating a [[Path]] that can have further segments added.
    */
  sealed trait Open

  /** Phantom type indicating a [[Path]] that cannot have further segments
    * added.
    */
  sealed trait Closed

  /** The `Path` representing the root. You can start constructing paths using
    * `Path.root` but it is more idiomatic to call `/` directly on the `Path`
    * companion object.
    */
  final val root: Path[EmptyTuple, EmptyTuple, Open] =
    new Path[EmptyTuple, EmptyTuple, Open](Vector.empty, 0, Query.empty)

  /** Create a `Path` that matches the given segment. */
  def /(segment: String): Path[EmptyTuple, EmptyTuple, Open] =
    new Path[EmptyTuple, EmptyTuple, Open](
      Vector(Segment(segment)),
      0,
      Query.empty
    )

  /** Create a `Path` that matches the given literal segment. */
  def /(segment: Segment): Path[EmptyTuple, EmptyTuple, Open] =
    new Path[EmptyTuple, EmptyTuple, Open](Vector(segment), 0, Query.empty)

  /** Create a `Path` that matches all remaining segments. */
  def /(segments: Segments.type): Path[EmptyTuple, EmptyTuple, Closed] =
    new Path[EmptyTuple, EmptyTuple, Closed](Vector(segments), 0, Query.empty)

  /** Create a `Path` that matches a segment and extracts it as a parameter. */
  def /[A](param: Param[A]): Path[Tuple1[A], EmptyTuple, Open] =
    new Path[Tuple1[A], EmptyTuple, Open](Vector(param), 1, Query.empty)

  /** Create a `Path` that extracts all remaining segments as a parameter. */
  def /[A](params: Params[A]): Path[Tuple1[A], EmptyTuple, Closed] =
    new Path[Tuple1[A], EmptyTuple, Closed](Vector(params), 1, Query.empty)

  extension [P <: Tuple, Q <: Tuple](path: Path[P, Q, Open])

    /** Add a literal segment to this `Path`. */
    def /(segment: String): Path[P, Q, Open] =
      new Path[P, Q, Open](
        path.segments :+ Segment(segment),
        path.paramCount,
        path.query
      )

    /** Add a literal segment to this `Path`. */
    def /(segment: Segment): Path[P, Q, Open] =
      new Path[P, Q, Open](
        path.segments :+ segment,
        path.paramCount,
        path.query
      )

    /** Add a wildcard that matches all remaining segments to this `Path`. */
    def /(segments: Segments.type): Path[P, Q, Closed] =
      new Path[P, Q, Closed](
        path.segments :+ segments,
        path.paramCount,
        path.query
      )

    /** Add a segment that extracts a single parameter to this `Path`. */
    def /[B](param: Param[B]): Path[Tuple.Append[P, B], Q, Open] =
      new Path[Tuple.Append[P, B], Q, Open](
        path.segments :+ param,
        path.paramCount + 1,
        path.query
      )

    /** Add a segment that extracts all remaining parameters to this `Path`. */
    def /[B](params: Params[B]): Path[Tuple.Append[P, B], Q, Closed] =
      new Path[Tuple.Append[P, B], Q, Closed](
        path.segments :+ params,
        path.paramCount + 1,
        path.query
      )

    /** Add query parameters to this `Path`. */
    def :?[B <: Tuple](query: Query[B]): Path[P, B, Closed] =
      new Path[P, B, Closed](path.segments, path.paramCount, query)

  /** This contains detailed descriptions of why a Path can fail, and utilities
    * to construct `ParseFailure` instances and raise them.
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
          |remaining segments use Segments. To match and capture all remaining
          |segments use Params.seq or another variant that captures all
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

    def paramMismatch(error: DecodeFailure) =
      ParseFailure(
        ParseStage.Uri,
        "A URI segment does not match a parameter",
        s"""This Path is expecting a segment to match the Param
           |${error.description}. However the URI contained the segment
           |${error.input} which does not match.""".stripMargin
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
