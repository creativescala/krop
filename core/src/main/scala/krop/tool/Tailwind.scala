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

object Tailwind {
  enum Os {
    case Windows
    case Linux
    case MacOs
  }

  enum Arch {
    case x64
    case arm64
  }

  val os: Os = {
    val osName = System.getProperty("os.name")
    if osName.contains("Win") then Os.Windows
    else if osName.contains("Linux") then Os.Linux
    else if osName.contains("Mac") then Os.MacOs
    else
      throw new IllegalArgumentException(
        s"The os.name property of ${osName} is not one recognized by this tool. Please file a bug report."
      )
  }

  val arch: Arch = {
    val osArch = System.getProperty("os.arch")
    if osArch.contains("amd64") then Arch.x64
    else if osArch.contains("aarch64") then Arch.arm64
    else
      throw new IllegalArgumentException(
        s"The os.arch property of ${osArch} is not one recognized by this tool. Please file a bug report."
      )
  }
}
