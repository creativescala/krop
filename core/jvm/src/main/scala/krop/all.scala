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

object all {
  export krop.Application
  export krop.Server
  export krop.ServerBuilder

  export krop.route.Route
  export krop.route.Request
  export krop.route.Response
  export krop.route.Path
  export krop.route.Param
  export krop.route.Segment
  export krop.route.Entity

  export krop.syntax.all.*

  export org.http4s.EntityDecoder
  export org.http4s.EntityEncoder
  export org.http4s.Method
  export org.http4s.Status

  import com.comcast.ip4s.*
  // Redefine these here because I don't know how to export an anonymous extension method
  extension (inline ctx: StringContext) {
    inline def ip(inline args: Any*): IpAddress =
      ${ Literals.ip('ctx, 'args) }

    inline def ipv4(inline args: Any*): Ipv4Address =
      ${ Literals.ipv4('ctx, 'args) }

    inline def ipv6(inline args: Any*): Ipv6Address =
      ${ Literals.ipv6('ctx, 'args) }

    inline def mip(inline args: Any*): Multicast[IpAddress] =
      ${ Literals.mip('ctx, 'args) }

    inline def mipv4(inline args: Any*): Multicast[Ipv4Address] =
      ${ Literals.mipv4('ctx, 'args) }

    inline def mipv6(inline args: Any*): Multicast[Ipv6Address] =
      ${ Literals.mipv6('ctx, 'args) }

    inline def ssmip(
        inline args: Any*
    ): SourceSpecificMulticast.Strict[IpAddress] =
      ${ Literals.ssmip('ctx, 'args) }

    inline def ssmipv4(
        inline args: Any*
    ): SourceSpecificMulticast.Strict[Ipv4Address] =
      ${ Literals.ssmipv4('ctx, 'args) }

    inline def ssmipv6(
        inline args: Any*
    ): SourceSpecificMulticast.Strict[Ipv6Address] =
      ${ Literals.ssmipv6('ctx, 'args) }

    inline def port(inline args: Any*): Port =
      ${ Literals.port('ctx, 'args) }

    inline def host(inline args: Any*): Hostname =
      ${ Literals.host('ctx, 'args) }

    inline def idn(inline args: Any*): IDN =
      ${ Literals.idn('ctx, 'args) }
  }
}
