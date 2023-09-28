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

[Request](request.md) and [Response][response.md] have separate pages, so here we'll just discuss the handler. There are three ways to create a handler, using `handle`, `handleIO`, or `passthrough`. Assuming the request produces a value of type `A` and the response needs a value of type `B`. Then these three methods have the following meaning:

- `handle` is a function `A => B`;
- `handle` is a function `A => IO[B]`; and
- `passthrough`, which can only be called when `A` is the same type as `B`, means that the output of the request is connected directly to the input of the response. This is useful, for example, when the response is loading a static file from the file system or the resources, and the request produces the name of the file to load.

