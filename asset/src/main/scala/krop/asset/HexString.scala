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
