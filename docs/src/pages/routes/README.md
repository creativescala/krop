# Routes

``` scala mdoc:invisible
import krop.all.*
```

`

Routes take care of the HTTP specific details of incoming requests and outgoing responses. Routes can:

1. match HTTP requests and extract Scala values;
2. convert Scala values into an HTTP response; and in the future
3. construct clients that call routes.


## Constructing Routes

Routes are constructed from three components:

1. a @:api(krop.route.Request), which describes a HTTP request;
2. a @:api(krop.route.Response), which describes a HTTP response; and
3. a handler, which processes the values extracted from the request and produces the value needed by the response.

The idiomatic way to construct a `Route` is by calling the `Route.apply` method, passing a @:api(krop.route.Request) and @:api(krop.route.Response), and then adding a handler.

Here is a small example illustrating the process.

``` scala mdoc:silent
val route = Route(Request.get(Path.root / "user" / Param.int), Response.ok[String])
  .handle(userId => s"You asked for the user ${userId}")
```

[Request](request.md) and [Response](response.md) have separate pages, so here we'll just discuss the handler. There are three ways to create a handler, using `handle`, `handleIO`, or `passthrough`. Assuming the request produces a value of type `A` and the response needs a value of type `B`. Then these three methods have the following meaning:

- `handle` is a function `A => B`;
- `handle` is a function `A => IO[B]`; and
- `passthrough`, which can only be called when `A` is the same type as `B`, means that the output of the request is connected directly to the input of the response. This is useful, for example, when the response is loading a static file from the file system or the resources, and the request produces the name of the file to load.


### Type Transformations for Handlers

If you dig into the types produced by `Requests`, you notice a tuple types are used. Here's an example, showing a `Request` producing a `Tuple2`.

``` scala mdoc
val request = Request.get(Path.root / Param.int / Param.string)
```

However, when you come to use a handler with such a request, you can use a normal function with two arguments *not* a function that accepts a single `Tuple2`.

``` scala mdoc:silent
Route(request, Response.ok[String])
  .handle((int, string) => s"${int.toString}: ${string}")
```

The conversion between tuples and functions is done by given instances of @api(krop.route.TupleApply).

There are several useful instances for functions of no arguments. In the case where a `Request` produces no values, any of the following will work:

- a function of no arguments;
- a function with a single `Unit` argument; and
- a function with a single `Any` argument.
