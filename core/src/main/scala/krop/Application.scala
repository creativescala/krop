package krop

import cats.effect.IO
import org.http4s.HttpApp

// Not implemented as an opaque type as it clashes with Route.Route
final case class Application(unwrap: HttpApp[IO])
