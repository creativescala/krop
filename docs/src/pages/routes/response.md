# Response

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Response) describes how to create a HTTP response from a Scala value. 
For example, the following `Response` will produce an HTTP OK response with a `text/plain` entity.

```scala mdoc:silent
val ok: Response[String] = Response.ok(Entity.text)
```

The entity will be constructed from a `String` that is passed to the `respond` method on the `Response`.
You usually won't do this yourself; it is handled by the `Route` the `Response` is part of.


## Entities

Entities (response bodies) are handled in the same way as [requests](request.md): by specifying an @:api(krop.route.Entity). In this case the `Entity` is responsible for encoding Scala values as data in the HTTP response.

Use `Entity.unit` to indicate that your response has no entity. For example:

```scala mdoc:silent
val noBody: Response[Unit] = Response.ok(Entity.unit)
```


## Headers

Headers can be added using the `withHeader` method. This method accepts one of more values in any of the following forms:

- A value of type `A` which has a `Header[A]` in scope
- A `(name, value)` pair of `String`, which is treated as a `Recurring`
  header
- A `Header.Raw`
- A `Foldable` (`List`, `Option`, etc) of the above.

In the example below we add a header using the `(name, value)` form, and a value with a `Header[A]` in scope.

```scala mdoc:silent
import org.http4s.headers.Allow

val headers = Response.ok(Entity.html).withHeader("X-Awesomeness" -> "10.0", Allow(Method.GET))
```


## Error Handling

In many cases you'll need to generate an error response despite a valid request. For example, you would generate a 404 Not Found if the client has sent a well-formed request but the requested resource doesn't exist. This situation can be handled using the `orNotFound` method, which converts a `Response[A]` to a `Response[Option[A]]`. When passed a `None` the `Response` responds with a 404. For more complex cases you can use `orElse`, which allows you to handle an `Either[A, B]` and introduce custom error handling.
