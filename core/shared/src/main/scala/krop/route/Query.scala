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

import scala.util.Success
import scala.util.Try

trait Query[A] {
  def parse(params: Map[String, List[String]]): Try[A]
  def unparse(a: A): Map[String, List[String]]

  def describe: String
}
object Query {
  def empty: Query[Unit] =
    new Query {
      def parse(params: Map[String, List[String]]): Try[Unit] = Success(())
      def unparse(a: Unit): Map[String, List[String]] = Map.empty
      val describe = ""
    }
}
