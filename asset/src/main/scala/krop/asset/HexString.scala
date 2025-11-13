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

package krop.asset

import fs2.hashing.Hash

import java.util.HexFormat

/** A hexadecimal formatted String */
opaque type HexString = String
extension (hex: HexString) {
  def value: String = hex
}
object HexString {
  private val hexFormat = HexFormat.of()

  def unsafeApply(string: String): HexString = string

  def fromHash(hash: Hash): HexString = {
    val array = Array.ofDim[Byte](hash.bytes.size)
    hash.bytes.copyToArray(array)

    hexFormat.formatHex(array)
  }
}
