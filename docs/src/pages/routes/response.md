# Response

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Response) describes how an HTTP response can be constructed from Scala values.

```scala mdoc:silent
val response = Response.ok(Entity.html)
```
