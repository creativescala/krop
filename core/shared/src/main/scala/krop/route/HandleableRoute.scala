package krop.route

import cats.effect.IO

/** The internal view of a route, exposing the types that a handler works with.
  */
trait HandleableRoute[E <: Tuple, R] extends BaseRoute {
  self: BaseRoute {
    def request: Request[?, ?, ?, E]
    def response: Response[R, ?]
  } =>
  import HandleableRoute.{HandlerIOBuilder, HandlerPureBuilder}

  def request: Request[?, ?, ?, E]
  def response: Response[R, ?]

  /** Handler incoming requests with the given function. */
  def handle(using ta: TupleApply[E, R]): HandlerPureBuilder[E, ta.Fun, R] =
    HandlerPureBuilder(this, ta)

  /** Handler incoming requests with the given function. */
  def handleIO(using ta: TupleApply[E, IO[R]]): HandlerIOBuilder[E, ta.Fun, R] =
    HandlerIOBuilder(this, ta)

  /** Pass the result of parsing the request directly the response with no
    * modification.
    */
  def passthrough(using pb: PassthroughBuilder[E, R]): Handler =
    Handler(this, pb.build)
}
object HandleableRoute {

  /** This class exists to help type inference when constructing a Handler from
    * a Route.
    */
  final class HandlerPureBuilder[E <: Tuple, F, R](
      route: HandleableRoute[E, R],
      ta: TupleApply.Aux[E, F, R]
  ) {
    def apply(f: F): Handler = {
      val handle = ta.tuple(f)
      Handler(route, i => IO.pure(handle(i)))
    }
  }

  /** This class exists to help type inference when constructing a Handler from
    * a Route.
    */
  final class HandlerIOBuilder[E <: Tuple, F, R](
      route: HandleableRoute[E, R],
      ta: TupleApply.Aux[E, F, IO[R]]
  ) {
    def apply(f: F): Handler = Handler(route, ta.tuple(f))

  }
}
