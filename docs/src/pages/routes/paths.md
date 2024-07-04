# Paths

```scala mdoc:invisible
import krop.all.*
```

A @:api(krop.route.Path) represents a pattern to match against the path component of the request's URI. `Paths` are created by calling the `/` method on a `Path` to add segments to the pattern. For example

```scala mdoc:silent
Path / "user" / "create"
```

matches the path `/user/create`.

To create a path without any segments you can use `Path.root`.


## Capturing Path Segments

Use a @:api(krop.route.Param) to capture part of the path for use by the handler.
For example

```scala mdoc:silent
Path / "user" / Param.int / "view"
```

matches `/user/<id>/view`, where `<id>` is an `Int`, and makes the `Int`
value available to the request handler.


## Matching All Segments

A `Path` will fail to match if the URI's path has more segments than the
`Path` matches. So `Path / "user" / "create"` will not match
`/user/create/1234`. Use `Segment.all` to match and ignore all the segments
to the end of the URI's path. For example

```scala mdoc:silent
Path / "assets" / Segment.all
```

will match `/assets/`, `/assets/example.css`, and `/assets/css/example.css`.

To capture all segments to the end of the URI's path, use an instance of
`Param.All` such as `Param.seq`. So

```scala mdoc:silent
Path / "assets" / Param.seq
```

will capture the remainder of the URI's path as a `Seq[String]`.

A `Path` that matches all segments is called a closed path. Attempting to add an
element to a closed path will result in an exception.

```scala mdoc:crash
Path / Segment.all / "crash"
```


## Capturing Query Parameters

A `Path` can also match and capture query parameters. For instance, the following path captures the query parameter `id` as an `Int`.

```scala mdoc:silent
Path / "user" :? Query("id", Param.int)
```

Multiple parameters can be captured. This example captures an `Int` and `String`.

```scala mdoc:silent
Path / "user" :? Query("id", Param.int).and("name", Param.string)
```

There can be multiple parameters with the same name. How this is handled depends on the underlying `Param`. A `Param` that captures only a single element, such as `Param.int` or `Param.string`, will only capture the first of multiple parameters. A `Param` that captures multiple elements, such as `Param.seq` will capture all the parameters with the given name. For example, this will capture all parameters called `name`, producing a `Seq[String]`.

```scala mdoc:silent
Path / "user" :? Query("name", Param.seq)
```

A parameter can be optional. To indicate this we need to work directly with @:api(krop.route.QueryParam), which has so far been hidden by convenience methods in the examples above.

Constructing a `QueryParam` requires a name and a `Param`, which is the same as we've seen above.

```scala mdoc:silent
val param = QueryParam("id", Param.int)
```

We can also call the `optional` constructor on the `QueryParam` companion object to create an optional query parameter. Optional parameters don't cause a route to fail to match if the parameter is missing. Instead `None` is returned.

```scala mdoc:silent
val optional = QueryParam.optional("id", Param.int)
```

To collect all the query parameters as a `Map[String, List[String]]` use `QueryParam.all`.

```scala mdoc:silent
val all = QueryParam.all
```


### Query Parameter Semantics

Query parameter semantics can be quite complex. There are four cases to consider:

1. A parameter exists under the given name and the associated value can be parsed.
2. A parameter exists under the given name and the associated value cannot be parsed.
3. A parameter exists under the given name but there is no associated value.
4. No parameter exists under the given name.

The first case is the straightforward one where query parameter parsing always succeeds.

```scala mdoc:reset:invisible
import krop.all.*
```
```scala mdoc:silent
val required = QueryParam("id", Param.int)
val optional = QueryParam.optional("id", Param.int)
```
```scala mdoc
required.parse(Map("id" -> List("1")))
optional.parse(Map("id" -> List("1")))
```

In the second case both required and optional query parameters fail.

```scala mdoc
required.parse(Map("id" -> List("abc")))
optional.parse(Map("id" -> List("abc")))
```

A required parameter will fail in the third case, but an optional parameter will succeed with `None`.

```scala mdoc
required.parse(Map("id" -> List()))
optional.parse(Map("id" -> List()))
```

Similarly, a required parameter will fail in the third case but an optional parameter will succeed with `None`.

```scala mdoc
required.parse(Map())
optional.parse(Map())
```


## Params

There are a small number of predefined `Param` instances on the
@:api(krop.route.Param$) companion object. Constructing your own instances can
be done in several ways.

The `imap` method transforms a `Param[A]` into a `Param[B]` by providing
functions `A => B` and `B => A`. This example constructs a `Param[Int]` from the
built-in `Param[String]`.

```scala mdoc:silent
val intParam = Param.string.imap(_.toInt)(_.toString)
```
```scala mdoc
intParam.parse("100")
```

A `Param.One[A]` can be lifted to a `Param.All[Seq[A]]` that uses the given
`Param.One` for every element in the `Seq`.

```scala mdoc:silent
val intParams = Param.lift(intParam)
```
```scala mdoc
intParams.unparse(Seq(1, 2, 3))
```

The `mkString` method can be used for a `Param.All` that constructs a `String`
containing elements separated by a separator. For example, to accumulate a
sub-path we could use the following.

```scala mdoc:silent
val subPath = Param.mkString("/")
```
```scala mdoc
subPath.parse(Vector("assets", "css"))
subPath.unparse("assets/css")
```

Finally, you can directly call the constructors for `Param.One` and `Param.All`.


### Param Names

`Params` have a `String` name. This is, by convention, some indication of the type written within angle brackets. For example `"<String>"` for a `Param[String]`.

```scala mdoc
Param.string.name
```

The name is mosty used in development mode, to output useful debugging information. You can change the name of a `Param` using the `withName` method. It's good practice to set the name whenever you create a new `Param`. For example, if deriving a new `Param` from an existing one you should consider changing the name.

```scala mdoc
// Bad, as the name doesn't reflect the underlying type.
intParam.name

// Better, as the name has been changed appropriately.
intParam.withName("<Int>").name
```
