# Server

```scala mdoc:invisible
import cats.effect.IO
import krop.all.*
```

A @:api(krop.Server), unsurprisingly, runs a web application. Every Krop application needs a `Server`, which is usually constructed via a @:api(krop.ServerBuilder).

Using a `ServerBuilder` can be as simple as

```scala mdoc:silent
val app: Application = ???

val builder = ServerBuilder.default.withApplication(app)
```

This uses the default settings (localhost and port 8080). The are builder methods that allow these to be changed.

Once the builder options have been set, calling the `run` method will construct a `Server` and immediately run it.

```scala 
// If this wasn't just documentation you'd now have a server listening on port 8080.
builder.run()
```

A `ServerBuilder` can also be converted a `Server` using the `build` method.

```scala mdoc:silent
val server: Server = builder.build
```

A `Server` can then be `run`, or converted to an `IO[Unit]`  using `toIO`.

```scala
// Run the server immediately.
server.run()
```
```scala mdoc:silent
// This IO can be run by an IOApp, for example
val io: IO[Unit] = server.toIO
```
