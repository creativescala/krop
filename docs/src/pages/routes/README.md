# Routing

```scala mdoc:invisible
import krop.all.*
```

Routing handles the HTTP specific details of incoming requests and outgoing responses. Routing can:

1. match HTTP requests and extract Scala values;
2. convert Scala values to an HTTP response; and
3. construct clients that do the above in reverse.

There are two main abstractions:

- a @:api(krop.route.Route), which deals with a single request and response; and
- a @:api(krop.route.Routes), which is a collection of @:api(krop.route.Route).


## Constructing A Route

A @:api(krop.route.Route) is constructed from three components:

1. a @:api(krop.route.Request), which describes a HTTP request;
2. a @:api(krop.route.Response), which describes a HTTP response; and
3. a handler, which processes the values extracted from the request and produces the value needed by the response.

The idiomatic way to construct a `Route` is by calling the `Route.apply` method, passing a @:api(krop.route.Request) and @:api(krop.route.Response), and then adding a handler to the resulting object.

Here is a small example showing the process.

```scala mdoc:silent
val route = Route(Request.get(Path / "user" / Param.int), Response.ok(Entity.text))
  .handle(userId => s"You asked for the user ${userId.toString}")
```

[Request](request.md) and [Response](response.md) have separate pages, so here we'll just discuss the handler. There are three ways to create a handler: using `handle`, `handleIO`, or `passthrough`. Assume the request produces a value of type `A` and the response needs a value of type `B`. Then these three methods have the following meaning:

- `handle` is a function `A => B`;
- `handle` is a function `A => IO[B]`; and
- `passthrough`, which can only be called when `A` is the same type as `B`, means that the output of the request is connected directly to the input of the response. This is useful, for example, when the response is loading a static file from the file system, and the request produces the name of the file to load.


### Type Transformations for Handlers

If you dig into the types produced by `Requests`, you notice a lot of tuple types are used. Here's an example, showing a `Request` producing a `Tuple2`.

```scala mdoc
val request = Request.get(Path / Param.int / Param.string)
```

However, when you come to use a handler with such a request, you can use a normal function with two arguments *not* a function that accepts a single `Tuple2`.

```scala mdoc:silent
Route(request, Response.ok(Entity.text))
  .handle((int, string) => s"${int.toString}: ${string}")
```

The conversion between tuples and functions is done by given instances of @:api(krop.route.TupleApply), which allows a function `(A, B, ..., N) => Z` to be applied to a tuple `(A, B, ..., N)`


## Reverse Routing

Given a @:api(krop.route.Route) you can construct a hyperlink to that route using the `pathTo` method. Here's an example.
We first create a @:api(krop.route.Route).

```scala mdoc:silent
val viewUser = Route(Request.get(Path / "user" / Param.int), Response.ok(Entity.text))
  .handle(userId => s"You asked for the user ${userId.toString}")
```

Now we can call `pathTo` to construct a path to that route, which we could embed in an HTML form or a hyperlink.

```scala mdoc
viewUser.pathTo(1234)
```

Note that we pass to `pathTo` the parameters for the @:api(krop.route.Path) component of the route.
If the route has no path parameters there is an overload with no parameters.

```scala mdoc:silent
val users = Route(Request.get(Path / "users"), Response.ok(Entity.text))
  .handle(() => "Here are the users.")
```
```scala mdoc
users.pathTo
```

If there is more than one parameter we must collect them in a tuple.

```scala mdoc:silent
val twoParams = Route(Request.get(Path / "user" / Param.int / Param.string), Response.ok(Entity.text))
  .handle((userId, name) => s"User with id ${userId} and name ${name}.")
```
```scala mdoc
twoParams.pathTo(1234, "McBoopy")
```


## Combining Routes

Two or more routes can be combined using the `orElse` method, creating @:api(krop.route.Routes).

``` scala mdoc
val routes = viewUser.orElse(users).orElse(twoParams)
```

A `Route` or `Routes` can also be combined with an `Application` using overloads of the `orElse` method, which produces an `Application`.
