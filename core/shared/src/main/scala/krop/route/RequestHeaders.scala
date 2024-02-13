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

import org.http4s.Header

final case class RequestHeaders[H <: Tuple](headers: Vector[Header[?, ?]]) {

  /** Add the given header to the headers extracted from the request. */
  def withHeader[A](header: A)(using
      h: Header[A, ?]
  ): RequestHeaders[Tuple.Append[H, A]] =
    this.copy(headers = headers :+ h)
}
object RequestHeaders {
  val empty: RequestHeaders[EmptyTuple] = RequestHeaders(Vector.empty)

  def apply[A](header: A)(using h: Header[A, ?]): RequestHeaders[Tuple1[A]] =
    empty.withHeader(header)
}
