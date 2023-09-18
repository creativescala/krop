package krop

import cats.effect.IO
import org.http4s.HttpApp

object Application {
  opaque type Application = HttpApp[IO]
  extension (app: Application) {

    /** Expose the underlying implementation of this type */
    def unwrap: HttpApp[IO] =
      app
  }
  object Application {
    def apply(http: HttpApp[IO]): Application =
      http
  }
}
