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

/** The internal view of a route, exposing the types that a handler works with.
  *
  * Just exposes the types, so that other types that want to purely with the
  * types without the API can do so.
  */
trait InternalRoute[E <: Tuple, R] extends BaseRoute {
  def request: Request[?, ?, ?, E]
  def response: Response[R, ?]
}
