# Response

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Response) describes how to create a HTTP response from a Scala value. 
For example, the following `Response` will produce an HTTP OK response with a `text/plain` entity.

```scala mdoc:silent
val ok: Response[String, String] = Response.ok(Entity.text)
```

The entity will be constructed from a `String` that is passed to the `respond` method on the `Response`.
You usually won't do this yourself; it is handled by the `Route` the `Response` is part of.


## Entities

Entities (response bodies) are handled in the same way as [requests](request.md): by specifying an @:api(krop.route.Entity). In this case the `Entity` is responsible for encoding Scala values as data in the HTTP response.

Use `Entity.unit` to indicate that your response has no entity. For example:

```scala mdoc:silent
val noBody: Response[Unit, Unit] = Response.ok(Entity.unit)
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

val headers = 
  Response.ok(Entity.html).withHeader("X-Awesomeness" -> "10.0", Allow(Method.GET))
```


## Error Handling

In many cases you'll need to generate an error response despite a valid request. For example, you could generate a 404 Not Found if the client has sent a well-formed request for a user but that user does not exist. This situation can be handled using the `orNotFound` method, which converts a `Response[A]` to a `Response[Option[A]]`. When passed a `None` the `Response` responds with a 404. This is shown in the example below. If the user ID is not 1 (the only valid ID) a 404 will be returned.

```scala mdoc:silent
val getUser = 
  Route(
    Request.get(Path / "user" / Param.int),
    Response.ok(Entity.text).orNotFound
  ).handle(id => 
    if id == 1 then Some("Found the user!") else None
  )
```

For more complex cases you can use `orElse`, which allows you to handle an `Either[A, B]` and introduce custom error handling. The example below shows complex error handling combining `orElse` and `orNotFound`. A 404 Not Found is returned if the user id does not correspond to an existing user, and a 400 Bad Request is returned if the `Name` [entity](entities.md) is not valid.

```scala mdoc:silent
import io.circe.{Decoder, Encoder}

final case class Name(name: String) derives Decoder, Encoder
final case class User(id: Int, name: String) derives Decoder, Encoder

val postUser = 
  Route(
    Request.post(Path / "user" / Param.int).withEntity(Entity.jsonOf[Name]),
    Response.ok(Entity.jsonOf[User])
      .orElse(Response.status(Status.BadRequest, Entity.text))
      .orNotFound
  ).handle((id, name) => 
    // Check ID is valid
    if id == 1 then
      // Check name is valid
      if name.name == "Hieronymus Bosch" then Some(Right(User(1, name.name)))
      else Some(Left("$name is not an allowed name"))
    else None
  )
```

Not that when using `orElse` the *first* `Response` is the successful one, and the second is the error case. This follows the usual convention in English, but means that when written the `Response` on the left-hand side corresponds to a `Right` value. In other words, we write

```scala
success.orElse(error)
```

not

```scala
error.orElse(success)
```

