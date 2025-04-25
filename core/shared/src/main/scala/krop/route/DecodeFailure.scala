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

/** Represents a failure to decode a value
  *
  * @param input
  *   The input that we attempted to decode.
  * @param description
  *   A description of what was expected from the input. By convention this is
  *   the name of the type we expected to decode to.
  */
final case class DecodeFailure(input: String | Seq[String], description: String)
