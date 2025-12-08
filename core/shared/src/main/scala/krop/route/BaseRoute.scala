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

/** A BaseRoute indicates that something is a route, and has a Request and a
  * Response. The types of the Request and Response are erased, so this is only
  * useful for runtime introspection. Other types keep more information and are
  * useful for other cases.
  */
trait BaseRoute {
  def request: Request[?, ?, ?, ?]
  def response: Response[?, ?]
}
