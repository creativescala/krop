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

package krop.tool

import scalatags.Text.TypedTag
import scalatags.Text.all.*

object TurboStream {
  val source: TypedTag[String] = tag("turbo-stream-source")
  val stream: TypedTag[String] = tag("turbo-stream")
  val template: TypedTag[String] = tag("template")

  object action {
    val append = scalatags.Text.all.action := "append"
    val prepend = scalatags.Text.all.action := "prepend"
    val replace = scalatags.Text.all.action := "replace"
    val update = scalatags.Text.all.action := "update"
    val remove = scalatags.Text.all.action := "remove"
    val before = scalatags.Text.all.action := "before"
    val after = scalatags.Text.all.action := "after"
  }
}
