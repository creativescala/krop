# Response

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Response) describes how a HTTP response can be constructed from Scala values. A response can be anything that implements the `Response` trait, but the usual way to call one of the constructors on the @:api(krop.route.Response$) companion object. For example

```scala mdoc:silent
val response = Response.ok(Entity.html)
```

constructs a `Response` that responds with an HTTP OK, and an HTML entity constructed from a Scala `String`.
