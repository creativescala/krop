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

/** Failure raised when query parsing fails. */
enum QueryParseFailure(val message: String) {

  /** Query parameter parsing failed because no parameter with the given name
    * was found in the query parameters.
    */
  case NoParameterWithName(name: String)
      extends QueryParseFailure(
        s"There was no query parameter with the name ${name}."
      )

  /** Query parameter parsing failed because there was a parameter with the
    * given name in the query parameters, but that parameter was not associated
    * with any values.
    */
  case NoValuesForName(name: String)
      extends QueryParseFailure(
        s"There were no values associated with the name ${name}"
      )

  case ValueParsingFailed(name: String, value: String, description: String)
      extends QueryParseFailure(
        s"Parsing the value ${value} as ${description} failed for the query parameter ${name}"
      )
}
