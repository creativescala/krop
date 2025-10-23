# Routing

```scala mdoc:invisible
import krop.all.*
```

Routing handles the HTTP specific details of incoming requests and outgoing responses. The main uses of routes are to:

1. match HTTP requests and extract Scala values;
2. convert Scala values to an HTTP response; and
3. reversing a route to create a link to the route or a client that calls the route.

A @:api(krop.route.Route) describes how to convert an incoming request into Scala values, and how to turn Scala values in an outgoing response. A route is purely a description. It doesn't do anything with a request, or produce a response, until it is converted to a @:api(krop.route.Handler).

A @:api(krop.route.Handler) can handle an HTTP request and, if it accepts the request, produce a response. Handlers can also create resources that live for the lifetime of the web server, which allows for implementing caches and the like. A @:api(krop.route.Handlers) is a collection of `Handler`.


## The Route Type

The `Route` type is fairly complex, though you can ignore this in most uses.

``` scala
Route[P <: Tuple, Q <: Tuple, I <: Tuple, O <: Tuple, R]
```

The types have the following meanings:

* `P` is the type of values extracted from the request's path by the @:api(krop.route.Path).
* `Q` is the type of query parameters extracted by the @:api(krop.route.Path).
* `I` is the type of all values extracted from the HTTP request.
* `O` is the type of values to construct an HTTP request to this `Route`. This is often, but not always, the same as `I`.
* `R` is the type of the value to construct an HTTP response.

Most of these types are tuples because they accumulate values extracted from smaller components of the HTTP request.
This will become clearer in the examples below.


## Constructing A Route

A @:api(krop.route.Route) is constructed from two components:

1. a @:api(krop.route.Request), which describes a HTTP request;
2. a @:api(krop.route.Response), which describes a HTTP response; and

The idiomatic way to construct a `Route` is by calling the `Route.apply` method, passing a @:api(krop.route.Request) and @:api(krop.route.Response). 
Here is a small example.

```scala mdoc:silent
val route = 
  Route(
    Request.get(Path / "user" / Param.int), 
    Response.ok(Entity.text)
  )
```

This route tells us two things:

1. It will match an incoming GET request to a path like `/user/1234` and extract the number as an `Int`.
2. It convert a `String` to an OK response. The response will include that `String` as the response's entity, and it will have a content type `text/plain`.

To actually use this route we need to add a handler. In this case a handler would be either a function with type `Int => String` or with type `Int => IO[String]`. Here's a very simple example using an `Int => String` handler.

```scala
route.handle(userId => s"You asked for user $userId")
```

Notice that adding a handler produces a value with a different type, a @:api(krop.route.Handler).

For more details see the separate pages for [Request](request.md), [Response](response.md) and [Handler](../handler.md).



## Reverse Routing

There are three forms of reverse routing:

* constructing a `String` that corresponds to the path matched by the `Route`;
* constructing a `String` corresponding to the path and query parameters matched by the `Route`;
* constructing a HTTP request that will be matched by the `Route`.


### Reverse Routing for Paths

Given a @:api(krop.route.Route) you can construct a `String` containing the path to that route using the `pathTo` method. This can be used, for example, to embed hyperlinks to routes in generated HTML. Here's an example.

We first create a @:api(krop.route.Route).

```scala mdoc:silent
val viewUser = Route(Request.get(Path / "user" / Param.int), Response.ok(Entity.text))
```

Now we can call `pathTo` to construct a path to that route, which we could embed in an HTML form or a hyperlink.

```scala mdoc
viewUser.pathTo(1234)
```

Note that we pass to `pathTo` the parameters for the @:api(krop.route.Path) component of the route.
If the route has no path parameters there is an overload with no parameters.
Here's an example with no parameters.

```scala mdoc:silent
val users = Route(Request.get(Path / "users"), Response.ok(Entity.text))
```

Now we can call `pathTo` without any parameters.

```scala mdoc
users.pathTo
```

If there is more than one parameter we must collect them in a tuple.
The route below has two parameters.

```scala mdoc:silent
val twoParams = Route(Request.get(Path / "user" / Param.int / Param.string), Response.ok(Entity.text))
```

Notice when we call `pathTo` we pass a `Tuple2`.

```scala mdoc
twoParams.pathTo((1234, "McBoopy"))
```


### Reverse Routing for Paths and Queries

You can use the `pathAndQueryTo` method to construct a `String` contains both the path and query parameters to a @:api(krop.route.Route).

Here's an example of a @:api(krop.route.Route) that extracts elements from both the path and the query parameters.

```scala mdoc:silent
val searchUsers = Route(
  Request.get(
    Path / "users" / "search" / Param.string :? Query[Int]("start").and[Int]("stop")
  ),
  Response.ok(Entity.text)
)
```

```scala mdoc
searchUsers.pathAndQueryTo("scala", (1, 10))
```



## Combining Routes

Two or more routes can be combined using the `orElse` method, creating @:api(krop.route.Routes).

``` scala mdoc
val routes = viewUser.orElse(users).orElse(twoParams)
```

A `Route` or `Routes` can also be combined with an `Application` using overloads of the `orElse` method, which produces an `Application`.
