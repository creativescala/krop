# Request

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Request) describes a pattern within an HTTP request that a @:api(krop.route.Route) attempts to match. It can also create an HTTP request that the `Request` will match. This so-called *reverse routing* allows creating clients that will call an endpoint.

A `Request` always matches an HTTP method and a [Path](paths.md). It may match other components of a request as well. It may also extract elements from the path, query parameters, and other parts of the request. These are passed to the `Route` handler.

## The Request Type

You can usually avoid writing @:api(krop.route.Request) types explicitly, but in case you have to write them down a @:api(krop.route.Request) is defined as

```scala
Request[P <: Tuple, Q <: Tuple, I <: Tuple, O <: Tuple]
```

where

* `P` is the type of any path parameters extracted from the @:api(krop.route.Path); 
* `Q` is the type of any query parameters extracted from the @:api(krop.route.Path); 
* `I` is the type of all the values extracted from the @:api(krop.route.Request); and
* `O` is all type of all the values need to construct a HTTP request matched by this @:api(krop.route.Request).


## Creating Requests

`Requests` are created by calling the method on the `Request` object that corresponds to the HTTP method of interest. For example, `Request.get` for a GET request, `Request.post` for a POST request, and so on. Thesse methods all accept a @:api(krop.route.Path), as described in the [next section](paths.md), which is the only required part of a `Request` in addition to the HTTP method.

Here is an example that matches a GET request to the path `/user/<int>`, where `<int>` is an integer.

```scala mdoc:silent 
Request.get(Path / "user" / Param.int)
```

Working with paths is quite complex, so this has [it's own documentation](paths.md).

You can optionally match and extract values from the headers and entity of a HTTP request. If you want to extract match or extract values from the headers, you must call these methods before you call methods that deal with the entity. This design makes it a bit easier to deal with the types inside @:api(krop.route.Request).


### Dealing with Headers

You can extract the value of any particular header in the HTTP request, and make that value available to the request handler. Alternatively you can ensure that the header exists and has a particular value, but not make that value availabe to the handler.

There are two variants of the `extractHeader` method, which will get the value of a header and make it available to the handler. In the first variant you specify just the type of the header, which is usually found in the `org.https4s.headers` package.

Here is an example the extracts the value associated with the `Content-Type` header.

```scala mdoc:silent
import org.http4s.headers.*

Request.get(Path / "user" / Param.int)
  .extractHeader[`Content-Type`]
```

If we want to extract more than one header we call `andExtractHeader` for each additional header after the first.

```scala mdoc:silent
Request.get(Path / "user" / Param.int)
  .extractHeader[`Content-Type`]
  .andExtractHeader[Referer]
```

This variant of `extractHeader` requires us to specify a value for the header when we do reverse routing. We can avoid this by providing a header when we call `extractHeader`.

In this example we construct a JSON `Content-Type` header and pass that value to `extractHeader`. Now `jsonContentType` will be used when constructing an HTTP request matching this request.

```scala mdoc:silent
import org.http4s.MediaType

val jsonContentType = `Content-Type`(MediaType.application.json)

Request.get(Path / "user" / Param.int)
  .extractHeader(jsonContentType)
```

We often want to ensure that a header matches a particular value, but don't want to otherwise do anything with the value. In other words, we don't want to the header's value passed to the handler once we have verified it exists. For these cases we can use `ensureHeader`.

```scala mdoc:silent
Request.get(Path / "user" / Param.int)
  .ensureHeader(jsonContentType)
```

As with `extractHeader`, we use `andEnsureHeader` to ensure two or more headers.

Finally, not that although we've used content type headers in the examples you don't normally have to deal with them. If you specify a @:api(krop.route.Entity) that will check the headers are correct. We've used them in this examples as they are probably the headers that are most familiar to most web developers.


## Entities

Calling the `withEntity` method on a `Request` allows one to specify an @:api(krop.route.Entity), which is responsible for extracting data from an HTTP request. The `Entity` is responsible for checking the HTTP Content-Type header, and, if it matches, decoding the HTTP entity into a Scala value.

Here's an example of a `Request` that extracts HTML content as a `String` value.

```scala mdoc:silent
val html = Request.get(Path.root).withEntity(Entity.html)
```

There are several predefined `Entity` values on the companion object, but you can easily create your own if needed.
