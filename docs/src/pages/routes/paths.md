# Paths

A @:api(krop.route.Path) represents a pattern to match against the path
component of the request's URI. `Paths` are created starting with `Path.root`
and then calling the `/` method to add segments to the pattern. For example

```scala mdoc:silent
import krop.all.*

Path.root / "user" / "create"
```

matches a request with the path `/user/create`.


## Capturing Path Segments

Use a @:api(krop.route.Param) to capture part of the path for later processing.
For example

```scala mdoc:silent
Path.root / "user" / Param.int / "view"
```

matches `/user/<id>/view`, where `<id>` is an `Int`, and makes the `Int`
value available to the request handler.


## Matching All Segments

A `Path` will fail to match if the URI's path has more segments than the
`Path` matches. So `Path.root / "user" / "create"` will not match
`/user/create/1234`. Use `Segment.all` to match and ignore all the segments
to the end of the URI's path. For example

```scala mdoc:silent
Path.root / "assets" / Segment.all
```

will match `/assets/example.css` and `/assets/css/example.css`.

To capture all segments to the end of the URI's path, use an instance of
`Param.All` such as `Param.vector`. So

```scala mdoc:silent
Path.root / "assets" / Param.vector
```

will capture the remainder of the URI's path as a `Vector[String]`.

A `Path` that matches all segments is called a closed path. Attempting to add an
element to a closed path will result in an exception.

```scala mdoc:crash
Path.root / Segment.all / "crash"
```


## Params

There are a small number of predefined `Param` instances on the
@:api(krop.route.Param$) companion object. Constructing your own instances can
be done in several ways.

The `imap` method transforms a `Param[A]` into a `Param[B]` by providing
functions `A => B` and `B => A`. This example constructs a `Param[Int]` from the
built-in `Param[String]`.

```scala mdoc
val intParam = Param.string.imap(_.toInt)(_.toString)

intParam.parse("100")
```

A `Param.One[A]` can be lifted to a `Param.All[Vector[A]]` that uses the given
`Param.One` for every element in the `Vector`.

```scala mdoc
val intParams = Param.lift(intParam)

intParams.unparse(Vector(1, 2, 3))
```

The `mkString` method can be used for a `Param.All` that constructs a `String`
containing elements separated by a separator. For example, to accumulate a
sub-path we could use the following.

```scala mdoc
val subPath = Param.mkString("/")

subPath.parse(Vector("assets", "css"))
subPath.unparse("assets/css")
```

Finally, you can directly call the constructors for `Param.One` and `Param.All`.
