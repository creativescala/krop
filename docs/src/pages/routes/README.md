# Routing

```scala mdoc:invisible
import krop.all.*
```

Routing handles the HTTP specific details of incoming requests and outgoing responses. The main uses of routes are to:

1. match HTTP requests and extract Scala values;
2. convert Scala values to an HTTP response; and
3. reversing a route to create a link to the route or a client that calls the route.

A @:api(krop.route.Route) deals with a single request and response,
and a @:api(krop.route.Routes) is a collection of @:api(krop.route.Route).


## The Route Type

The `Route` type is fairly complex, though you can ignore this is most uses.

``` scala
final class Route[P <: Tuple, Q <: Tuple, I <: Tuple, O <: Tuple, R]
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

A @:api(krop.route.Route) is constructed from three components:

1. a @:api(krop.route.Request), which describes a HTTP request;
2. a @:api(krop.route.Response), which describes a HTTP response; and
3. a handler, which processes the values extracted from the request and produces the value needed by the response.

The idiomatic way to construct a `Route` is by calling the `Route.apply` method, passing a @:api(krop.route.Request) and @:api(krop.route.Response), and then adding a handler to the resulting object.
Here is a small example.

```scala mdoc:silent
val route = Route(Request.get(Path / "user" / Param.int), Response.ok(Entity.text))
  .handle(userId => s"You asked for the user ${userId.toString}")
```

This route will match, for example, a GET request to the path `/user/1234` and respond with the string `"You asked for the user 1234"`.

[Request](request.md) and [Response](response.md) have separate pages, so here we'll just discuss the handler. There are three ways to create a handler: using `handle`, `handleIO`, or `passthrough`. Assume the request produces a value of type `A` and the response needs a value of type `B`. Then these three methods have the following meaning:

- `handle` requires a function `A => B`;
- `handleIO` requires a function `A => IO[B]`; and
- `passthrough`, which can only be called when `A` is the same type as `B`, means that the output of the request is connected directly to the input of the response. This is useful, for example, when the response is loading a static file from the file system and the request produces the name of the file to load.


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
  .handle(userId => s"You asked for the user ${userId.toString}")
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
  .handle(() => "Here are the users.")
```

Now we can call `pathTo` without any parameters.

```scala mdoc
users.pathTo
```

If there is more than one parameter we must collect them in a tuple.
The route below has two parameters.

```scala mdoc:silent
val twoParams = Route(Request.get(Path / "user" / Param.int / Param.string), Response.ok(Entity.text))
  .handle((userId, name) => s"User with id ${userId} and name ${name}.")
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
    Path / "users" / "search" / Param.string :? Query("start", Param.int)
      .and("stop", Param.int)
  ),
  Response.ok(Entity.text)
).handle((term, start, stop) => s"Searching for users named $term, from page $start to $stop")
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
