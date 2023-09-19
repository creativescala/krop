package krop

import org.http4s.HttpRoutes
import cats.effect.IO
import cats.syntax.all.*
import org.http4s.Response
import cats.data.Kleisli

object Route {

  /** A [[krop.Route.Route]] is a function that accepts a request */
  opaque type Route = HttpRoutes[IO]
  extension (route: Route) {

    /** Expose the underlying implementation of this type */
    def unwrap: HttpRoutes[IO] =
      route

    /** Try this route. If it fails to match, try the other route. */
    def and(other: Route): Route =
      route <+> other

    /** Convert this [[krop.Route.Route]] into an [[krop.Application]] by
      * responding to all unmatched requests with a NotFound (404) response.
      */
    def orNotFound: Application =
      Application(
        Kleisli(req => route.unwrap.run(req).getOrElse(Response.notFound))
      )
  }
  object Route {
    def apply(route: HttpRoutes[IO]): Route =
      route
  }
}
