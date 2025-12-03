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

package krop

import cats.effect.IO
import org.http4s.server.websocket.WebSocketBuilder
import java.util.concurrent.atomic.AtomicInteger

/** Provides platform and server specific services and utilities that are
  * available after the http4s server has started.
  */
trait KropRuntime extends BaseRuntime {
  def webSocketBuilder: WebSocketBuilder[IO]
}

type WithRuntime[A] = KropRuntime => A

final class Key[V] private (val id: Int, val description: String) {
  def get(using runtime: KropRuntime): V = ???
  // runtime.getResource(this)

  override def hashCode(): Int = id
  override def equals(that: Any): Boolean =
    if that.isInstanceOf[Key[V]]
    then that.asInstanceOf[Key[V]].id == this.id
    else false
}
object Key {
  private val counter: AtomicInteger = AtomicInteger(0)

  private def nextId(): Int = {
    counter.getAndIncrement()
  }

  /** Creates a resource key without staging a resource. */
  def unsafe[V](description: String): Key[V] =
    Key(nextId(), description)
}
