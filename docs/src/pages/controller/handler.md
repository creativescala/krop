# Handlers

```scala mdoc:invisible
import cats.effect.IO
import krop.all.*
```

A @:api(krop.route.Route) extracts Scala values from an HTTP request, and converts Scala values into an HTTP response. A @:api(krop.route.Handler) adds the functionality that connects these two parts together, giving a complete controller that can be run as part of an application.

There are three ways to create a handler: using `handle`, `handleIO`, or `passthrough`. Assume the request produces a value of type `A` and the response needs a value of type `B`. Then these three methods have the following meaning:

- `handle` requires a function `A => B`;
- `handleIO` requires a function `A => IO[B]`; and
- `passthrough`, which can only be called when `A` is the same type as `B`, means that the output of the request is connected directly to the input of the response. This is useful, for example, when the response is loading a static file from the file system and the request produces the name of the file to load.

Let's say we have the following `Route`.

```scala mdoc:silent
val route = 
  Route(
    Request.get(Path / "user" / Param.int), 
    Response.ok(Entity.text)
  )
```

It produces an `Int` from the request, and requires a `String` to create the response. We can create a `Handler` in the following two ways:

```scala mdoc:silent
route.handle(userId => s"You asked for user $userId")
route.handleIO(userId => IO.pure(s"You asked for user $userId"))
```

We cannot use `passthrough` as the value produced from the request has a different type to the value required to create the response.


### Type Transformations for Handlers

If you dig into the types produced by `Request` you will notice a lot of tuple types are used. Here's an example, showing a `Request` producing a `Tuple2`.

```scala mdoc
val request = Request.get(Path / Param.int / Param.string)
```

This `Tuple2` arises because we extract two elements from the HTTP request's path: one `Int` and one `String`.
However, when you come to use a handler with such a request, you can use a normal function with two arguments *not* a function that accepts a single `Tuple2`.

```scala mdoc:silent
Route(request, Response.ok(Entity.text))
  .handle((int, string) => s"${int.toString}: ${string}")
```

The conversion between tuples and functions is done by given instances of @:api(krop.route.TupleApply), which allows a function `(A, B, ..., N) => Z` to be applied to a tuple `(A, B, ..., N)`
