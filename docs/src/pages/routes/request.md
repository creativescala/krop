# Request

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Request) describes a pattern within an HTTP request that a @:(krop.route.Route) attempts to match.

A `Request` always matches an HTTP method and a [Path](paths.md). It may match other components of a request as well. It may also extract elements from the path, query parameters, and other parts of the request. These are passed to the `Route` handler.


## Creating Requests

`Requests` are created by calling the method on the `Request` object that corresponds to the HTTP method of interest. For example, `Request.get` for a GET request, `Request.post` for a POST request, and so on. Thesse methods all accept a @:api(krop.route.Path), as described in the [next section](paths.md), which is the only part required path of a `Request` in addition to the HTTP method.

Builder methods on `Request` can be used to match and extract other parts of an HTTP request. The most common is to extract an HTTP entity, using the `withEntity` method.


## Entities

Calling the `withEntity` method on a `Request` allows one to specify an @:api(krop.route.Entity), which is responsible for extracting data from an HTTP request. The `Entity` is responsible for checking the HTTP Content-Type header, and, if it matches, decoding the HTTP entity into a Scala value.

Here's an example of a `Request` that extracts HTML content as a `String` value.

```scala mdoc:silent
val html = Request.get(Path.root).withEntity(Entity.html)
```

There are several predefined `Entity` values on the companion object, but you can easily create your own if needed.
