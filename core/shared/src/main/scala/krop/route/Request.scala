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

import cats.effect.IO
import cats.syntax.all.*
import krop.Types.TupleConcat
import krop.raise.Raise
import org.http4s.EntityDecoder
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Method
import org.http4s.Request as Http4sRequest

/** A [[krop.route.Request]] describes a pattern within a [[org.http4s.Request]]
  * that, if matched, will be routed to a handler. For example, it can look for
  * a particular HTTP method, say GET, and a particular pattern within a path,
  * such as "/user/create".
  *
  * The idiomatic way to create to a Request is starting with defining the HTTP
  * method and URI path, using the methods such as `get`, and `post` on the
  * companion object.
  *
  * @tparam P
  *   The type of values extracted from the URI path.
  * @tparam Q
  *   The type of values extracted from the URI query parameters.
  * @tparam I
  *   The type of values extracted from all parts of the request, including path
  *   and query.
  * @tparam O
  *   The type of values to create a [[org.http4s.Request]] that matches this
  *   [[krop.route.Request]].
  */
sealed abstract class Request[P <: Tuple, Q <: Tuple, I <: Tuple, O <: Tuple] {

  /** A type alias for the type of values this Request produces when it
    * successfully parses an incoming request.
    */
  type Parsed = I

  /** A type alias for the type of values this Request requires to construct an
    * incoming request.
    */
  type Unparsed = O

  //
  // Interpreters --------------------------------------------------------------
  //

  /** Given a [[org.http4s.Request]] and an error [[krop.raise.Handler]],
    * extract the requested values if possible or signal an error if the given
    * request does not match.
    */
  def parse[F[_, _]: Raise.Handler](
      req: Http4sRequest[IO]
  ): IO[F[ParseFailure, I]]

  /** Given appropriate values construct a [[org.http4s.Request]] that will
    * match this [[krop.route.Request]]. This can be used to construct calls to
    * the [[krop.route.Route]] that uses this [[krop.route.Request]].
    */
  def unparse(params: O): Http4sRequest[IO]

  /** Create a [[scala.String]] path suitable for embedding in HTML that links
    * to the path described by this [[package.Request]]. Use this to create
    * hyperlinks or form actions that call a route, without needing to hardcode
    * the route in the HTML.
    *
    * This path will not include settings like the entity or headers that this
    * [[package.Request]] may require. It is assumed this will be handled
    * elsewhere.
    */
  def pathTo(params: P): String

  /** Overload of `pathTo` for the case where the path has no parameters.
    */
  def pathTo(using ev: EmptyTuple =:= P): String =
    pathTo(ev(EmptyTuple))

  /** Overload of `pathTo` for the case where the path has a single parameter.
    */
  def pathTo[B](param: B)(using ev: Tuple1[B] =:= P): String =
    pathTo(ev(Tuple1(param)))

  /** Create a [[scala.String]] path suitable for embedding in HTML that links
    * to the path described by this [[package.Request]] and also includes query
    * parameters. Use this to create hyperlinks or form actions that call a
    * route, without needing to hardcode the route in the HTML.
    *
    * This path will not include settings like the entity or headers that this
    * [[package.Request]] may require. It is assumed this will be handled
    * elsewhere.
    */
  def pathAndQueryTo(pathParams: P, queryParams: Q): String

  /** Overload of `pathAndQueryTo` for the case where the path has no
    * parameters.
    */
  def pathAndQueryTo(queryParams: Q)(using ev: EmptyTuple =:= P): String =
    pathAndQueryTo(ev(EmptyTuple), queryParams)

  /** Overload of `pathAndQueryTo` for the case where the path has a single
    * parameter.
    */
  def pathAndQueryTo[B](pathParam: B, queryParams: Q)(using
      ev: Tuple1[B] =:= P
  ): String =
    pathAndQueryTo(ev(Tuple1(pathParam)), queryParams)

  /** Overload of `pathAndQueryTo` for the case where the query has a single
    * parameter.
    */
  def pathAndQueryTo[B](pathParams: P, queryParam: B)(using
      ev: Tuple1[B] =:= Q
  ): String =
    pathAndQueryTo(pathParams, ev(Tuple1(queryParam)))

  /** Overload of `pathAndQueryTo` for the case where the path and query have a
    * single parameter.
    */
  def pathTo[B, C](pathParam: B, queryParam: C)(using
      evP: Tuple1[B] =:= P,
      evQ: Tuple1[C] =:= Q
  ): String =
    pathAndQueryTo(evP(Tuple1(pathParam)), evQ(Tuple1(queryParam)))

  /** Produces a human-readable representation of this [[package.Request]]. The
    * toString method is used to output the usual programmatic representation.
    */
  def describe: String
}
object Request {
  //
  // Builder Impls -------------------------------------------------------------
  //

  // These classes form a finite-state machine for building a Request. The usual
  // way to use them is in a sequence:
  //
  // 1. Start by choosing a method and path
  // 2. Add headers (optional)
  // 3. Add entity (optional)

  final case class RequestMethodPath[P <: Tuple, Q <: Tuple](
      method: Method,
      path: Path[P, Q]
  ) extends Request[
        P,
        Q,
        TupleConcat[P, Q],
        TupleConcat[P, Q]
      ] {

    export path.pathTo
    export path.pathAndQueryTo

    def parse[F[_, _]: Raise.Handler](
        req: Http4sRequest[IO]
    ): IO[F[ParseFailure, Parsed]] = {
      IO.pure(
        Raise.handle(
          if req.method != method
          then
            Raise.raise(
              ParseFailure(
                ParseStage.Method,
                s"The request's method is not the expected method",
                s"Expected the request's method to be ${method}, but it was ${req.method}."
              )
            )
          else path.parse(req.uri)
        )
      )
    }

    def unparse(params: Unparsed): Http4sRequest[IO] = {
      val uri = path.unparse(params)
      Http4sRequest(uri = uri)
    }

    def describe: String =
      s"${method.toString()} ${path.describe}"

    /** Change the HTTP method that this `Request` matches to the given method.
      */
    def withMethod(
        method: Method
    ): RequestMethodPath[P, Q] =
      RequestMethodPath(method, path)

    /** Change the `Path` that this `Request` matches to the given path. */
    def withPath[P2 <: Tuple, Q2 <: Tuple](
        path: Path[P2, Q2]
    ): RequestMethodPath[P2, Q2] =
      RequestMethodPath(method, path)

    /** When matching a request, the given header must exist in the request and
      * it will be extracted and made available to the handler.
      *
      * When producing a request, a value of type `A` must be provided.
      */
    def extractHeader[A](using
        h: Header[A, ?],
        s: Header.Select[A]
    ): RequestHeaders[P, Q, Tuple1[s.F[A]], Tuple1[s.F[A]]] =
      RequestHeaders.empty(this).andExtractHeader[A]

    /** When matching a request, the given header must exist in the request and
      * it will be extracted and made available to the handler.
      *
      * When producing a request, the given header will be added to the request.
      */
    def extractHeader[A](header: A)(using
        h: Header[A, ?],
        s: Header.Select[A]
    ): RequestHeaders[P, Q, Tuple1[s.F[A]], EmptyTuple] =
      RequestHeaders.empty(this).andExtractHeader[A](header)

    /** When matching a request, the given header must exist in the request and
      * match the given value. It will not be made available to the handler.
      *
      * When producing a request, the given header will be added to the request.
      */
    def ensureHeader[A](header: A)(using
        h: Header[A, ?],
        s: Header.Select[A]
    ): RequestHeaders[P, Q, EmptyTuple, EmptyTuple] =
      RequestHeaders.empty(this).andEnsureHeader(header)

    def withEntity[D, E](
        entity: Entity[D, E]
    ): RequestEntity[P, Q, P, Q, D, E] = {
      val headers = RequestHeaders.empty(this)
      RequestEntity(headers, entity)
    }
  }

  // I is the type when this Request is viewed as an input. In other words, it's
  // the type passed to the handler.
  //
  // O is the type when this Request is viewed as an output. In other words,
  // it's the type of the value needed to construct a http4s request from this
  // Request.
  final case class RequestHeaders[
      P <: Tuple,
      Q <: Tuple,
      I <: Tuple,
      O <: Tuple
  ](
      path: RequestMethodPath[P, Q],
      headers: List[RequestHeaders.Process],
      // Count of values that will be provided to the handler
      inputCount: Int,
      // Count of values that must be supplied to construct a Request
      outputCount: Int
  ) extends Request[
        P,
        Q,
        TupleConcat[TupleConcat[P, Q], I],
        TupleConcat[TupleConcat[P, Q], O]
      ] {
    import RequestHeaders.Process
    import RequestHeaders.failure

    export path.pathTo
    export path.pathAndQueryTo

    def parse[F[_, _]: Raise.Handler](
        req: Http4sRequest[IO]
    ): IO[F[ParseFailure, Parsed]] = {
      val ioPQ: IO[F[ParseFailure, TupleConcat[P, Q]]] = path.parse(req)

      extension [A](opt: Option[A]) {
        def orFail(header: Header[?, ?]): Raise[ParseFailure] ?=> A =
          opt.getOrElse(
            Raise.raise(failure.headerNotFound(header.name.toString))
          )
      }

      val extracted: F[ParseFailure, List[?]] =
        Raise.handle { (r: Raise[ParseFailure]) ?=>
          val reqHeaders = req.headers
          headers.foldLeft(List.empty)((accum, p) =>
            p match {
              case Process.Extract(value, header, select) =>
                accum :+ reqHeaders.get(using select).orFail(header)
              case Process.ExtractFromName(header, select) =>
                accum :+ reqHeaders.get(using select).orFail(header)
              case Process.Ensure(value, header, select) =>
                reqHeaders.get(using select) match {
                  case None =>
                    Raise.raise(failure.headerNotFound(header.name.toString))
                  case s @ Some(actual) =>
                    if actual == value then accum
                    else
                      Raise.raise(
                        failure.headerDidntMatch(
                          header.name.toString,
                          actual,
                          value
                        )
                      )
                }
            }
          )
        }

      Raise.flatMapToIO(extracted) { e =>
        ioPQ.flatMap(fPQ =>
          Raise.mapToIO(fPQ)(pq =>
            IO.pure(
              (pq ++ Tuple.fromArray(e.toArray).asInstanceOf[I])
                .asInstanceOf[Parsed]
            )
          )
        )
      }
    }

    def unparse(params: Unparsed): Http4sRequest[IO] = {
      val ps = params.toIArray
      val (pqArr, oArr) = ps.splitAt(ps.length - outputCount)

      val req =
        path.unparse(Tuple.fromIArray(pqArr).asInstanceOf[TupleConcat[P, Q]])

      def loop(
          headers: List[Process],
          paramIdx: Int,
          result: Headers
      ): Headers =
        headers match {
          case Nil => result
          case (e: Process.Extract[a]) :: rest =>
            given Header[a, ?] = e.header
            loop(rest, paramIdx, result.put(e.value: a))

          case (e: Process.ExtractFromName[a]) :: rest =>
            given Header[a, ?] = e.header
            loop(rest, paramIdx + 1, result.put(oArr(paramIdx).asInstanceOf[a]))

          case (e: Process.Ensure[a]) :: rest =>
            given Header[a, ?] = e.header
            loop(rest, paramIdx, result.put(e.value))
        }

      val hs = loop(headers, 0, Headers.empty)

      Http4sRequest(uri = req.uri, headers = hs)
    }

    def describe: String =
      path.describe

    /** When matching a request, the given header must exist in the request and
      * it will be extracted and made available to the handler.
      *
      * When producing a request, a value of type `A` must be provided.
      */
    def andExtractHeader[A](using
        h: Header[A, ?],
        s: Header.Select[A]
    ): RequestHeaders[P, Q, Tuple.Append[I, s.F[A]], Tuple.Append[O, s.F[A]]] =
      RequestHeaders(
        path,
        headers :+ Process.ExtractFromName(h, s),
        inputCount + 1,
        outputCount + 1
      )

    /** When matching a request, the given header must exist in the request and
      * it will be extracted and made available to the handler.
      *
      * When producing a request, the given header will be added to the request.
      */
    def andExtractHeader[A](header: A)(using
        h: Header[A, ?],
        s: Header.Select[A]
    ): RequestHeaders[P, Q, Tuple.Append[I, s.F[A]], O] =
      RequestHeaders(
        path,
        headers :+ Process.Extract(header, h, s),
        inputCount + 1,
        outputCount
      )

    /** When matching a request, the given header must exist in the request and
      * match the given value. It will not be made available to the handler.
      *
      * When producing a request, the given header will be added to the request.
      */
    def andEnsureHeader[A](header: A)(using
        h: Header[A, ?],
        s: Header.Select[A]
    ): RequestHeaders[P, Q, I, O] =
      RequestHeaders(
        path,
        headers :+ Process.Ensure(header, h, s),
        inputCount,
        outputCount
      )
  }
  object RequestHeaders {
    enum Process {
      case Extract[A](value: A, header: Header[A, ?], select: Header.Select[A])
      case ExtractFromName[A](header: Header[A, ?], select: Header.Select[A])
      case Ensure[A](value: A, header: Header[A, ?], select: Header.Select[A])
    }

    object failure {
      def headerNotFound(name: String) =
        ParseFailure(
          ParseStage.Header,
          s"Could not extract the header ${name}",
          s"""The header named ${name} did not exist in the request's headers,
             |or the value could not be correctly parsed.""".stripMargin
        )

      def headerDidntMatch[A](name: String, actual: A, expected: A) =
        ParseFailure(
          ParseStage.Header,
          s"The header $name} did not have the expected value",
          s"""The header with name ${name} and value ${actual} was found in the request,
             |but we expected to find the value ${expected}.""".stripMargin
        )
    }

    def empty[P <: Tuple, Q <: Tuple](
        path: RequestMethodPath[P, Q]
    ): RequestHeaders[P, Q, EmptyTuple, EmptyTuple] =
      RequestHeaders(path, List.empty, 0, 0)
  }

  final case class RequestEntity[
      P <: Tuple,
      Q <: Tuple,
      I <: Tuple,
      O <: Tuple,
      D,
      E
  ](
      headers: RequestHeaders[P, Q, ?, ?],
      entity: Entity[D, E]
  ) extends Request[P, Q, Tuple.Append[I, D], Tuple.Append[O, E]] {

    def pathTo(params: P): String =
      headers.pathTo(params)

    def pathAndQueryTo(pathParams: P, queryParams: Q): String =
      headers.pathAndQueryTo(pathParams, queryParams)

    def describe: String =
      "${headers.describe} ${entity.encoder.contentType.map(_.mediaType).getOrElse(\"\")}"

    def parse[F[_, _]: Raise.Handler](
        req: Http4sRequest[IO]
    ): IO[F[ParseFailure, Parsed]] = {
      headers.parse(req).flatMap { result =>
        Raise.flatMapToIO(result) { i =>
          given EntityDecoder[IO, D] = entity.decoder
          req
            .as[D]
            .map { d =>
              Raise.succeed((i :* d).asInstanceOf[Parsed])
            }
            .handleErrorWith(err =>
              IO(
                Raise.handle(
                  Raise.raise(
                    ParseFailure(
                      ParseStage.Entity,
                      err.getMessage(),
                      err.getMessage()
                    )
                  )
                )
              )
            )
        }
      }
    }

    def unparse(params: Unparsed): Http4sRequest[IO] = {
      val (o, e) = params.splitAt(headers.outputCount)
      val request = headers.unparse(o.asInstanceOf[headers.Unparsed])
      val encoded = entity.encoder.toEntity(e.asInstanceOf[Tuple1[E]](0))
      request.withEntity(encoded)
    }

    def withEntity[D2, E2](
        entity: Entity[D2, E2]
    ): RequestEntity[P, Q, I, O, D2, E2] =
      RequestEntity(headers, entity)
  }

  def connect[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.CONNECT, path)

  def delete[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.DELETE, path)

  def get[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.GET, path)

  def head[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.HEAD, path)

  def options[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.OPTIONS, path)

  def patch[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.PATCH, path)

  def post[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.POST, path)

  def put[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.PUT, path)

  def trace[P <: Tuple, Q <: Tuple](
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    Request.method(Method.TRACE, path)

  def method[P <: Tuple, Q <: Tuple](
      method: Method,
      path: Path[P, Q]
  ): RequestMethodPath[P, Q] =
    RequestMethodPath(method, path)

}
