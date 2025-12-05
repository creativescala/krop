# Developing Krop

This section contains notes on developing Krop.


## Testing And All That

Use the sbt `build` task to compile, test, format, etc.


## Builders

We have a lot of builder methods. The convention is a method starting with `with` will return a copy with an updated value. E.g. `serverBuilder.withPort(...)` returns a copy of a `ServerBuilder` with an updated value of port.


## Wrapping http4s

We wrap a lot of http4s (and Cats and Cats Effect to a lesser extent) to make things simpler to use. For example, we like to get rid of the `IO` parameters that litter many http4s types. We don't abstract over effect types (and it, arguably, yields no value to do so) so don't need this.

Where we have a wrapper type, and it doesn't make sense to use an opaque type or a type alias, the value of the wrapped http4s type is, by convention, called `value`.


## Absolute Imports

We use absolute, not relative, imports. Relative imports require too much knowledge of the code base for the more casual reader.
