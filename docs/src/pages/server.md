# Server

```scala mdoc:invisible
import cats.effect.IO
import krop.all.*
```

## Creating a Server

A @:api(krop.Server), unsurprisingly, runs a web application. Every Krop application needs a `Server`, which is usually constructed via a @:api(krop.ServerBuilder).

Using a `ServerBuilder` can be as simple as

```scala mdoc:silent
val app: Application = Application.notFound

val builder = ServerBuilder.default.withApplication(app)
```

This uses the default settings (localhost and port 8080). The are builder methods that allow these to be changed.

Once the builder options have been set, calling the `run` method will construct a `Server` and immediately run it.

```scala 
// If this wasn't just documentation we'd now have a server listening on port 8080.
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


## Setting Server Options

Setting the server options, such as the port and host, are done by the builder methods on the `ServerBuilder`. Custom string contexts are provided for defining host and port. Concretely, this means writing code like

```scala mdoc:silent
ServerBuilder.default.withPort(port"4000")

ServerBuilder.default.withHost(host"127.0.0.1")
ServerBuilder.default.withHost(host"localhost")
```
