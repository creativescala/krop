# Application

An @:api(krop.Application) in what a Krop [server](server.md) runs, and thus building an `Application` is the end goal of working with Krop.

An `Application` consists of any number of [Routes](routes/README.md) followed by a catch-all that handles any requests not matched by a route. The usual catch-all is @:api(krop.tool.NotFound).
