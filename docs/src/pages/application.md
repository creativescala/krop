# Application

An @:api(krop.Application) in what a Krop [server](server.md) runs, and thus building an `Application` is the end goal of working with Krop.

An `Application` consists of any number of [Routes](routes/README.md) followed by a catch-all that handles any requests not matched by a route. The usual catch-all is @:api(krop.tool.Application.notFound).


## Development and Production Mode

Krop can run in one of two modes: development and production. In development
mode it shows output that is useful for debugging and otherwise inspecting
the running state. In production this output is hidden.

The mode is set by the `krop.mode` JVM system property. If it has the value of
"development" (without the quotes; any capitalization is fine) then the mode
is development. Otherwise it is production.

The mode is determined when Krop starts, and is available as the value of `krop.Mode`.
