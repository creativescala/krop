# Paths

```scala mdoc:invisible
import krop.all.{*, given}
```

A @:api(krop.route.Path) represents a pattern to match against the path component of the request's URI. `Paths` are created by calling the `/` method on a `Path` to add segments to the pattern. For example

```scala mdoc:silent
Path / "user" / "create"
```

matches the path `/user/create`.

To create a path without any segments you can use `Path.root`.


## Capturing Path Segments

Use a @:api(krop.route.Param) to capture part of the path for later use by the handler.
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
Path / "user" :? Query[Int]("id")
```

Multiple parameters can be captured. This example captures an `Int` and `String`.

```scala mdoc:silent
Path / "user" :? Query[Int]("id").and[String]("name")
```

There can be multiple parameters with the same name. How this is handled depends on the underlying @:api(krop.route.QueryParam). A `QueryParam` that captures only a single element, such as `QueryParam.int` or `QueryParam.string`, will only capture the first of multiple parameters. A `QueryParam` that captures multiple elements, created with `Query.all` will capture all the parameters with the given name. For example, this will capture all parameters called `name`, producing a `Seq[String]`.

```scala mdoc:silent
Path / "user" :? Query.all[Seq[String]]("name")
```

A parameter can be optional, which we can create with `Query.optional`. Optional parameters don't cause a route to fail to match if the parameter is missing. 

```scala mdoc:silent
Path / "user" :? Query.optional[String]("name") // Returns Option[String]
```

To collect all the query parameters as a `Map[String, List[String]]` use `Query.everything`.

```scala mdoc:silent
val everything = Query.everything
```

We can also construct a `QueryParam` directly, which requires a name and a type parameter, similarly to working with `Query`. The type parameter is used to find a given instance of @:api(krop.route.StringCodec) or @:api(krop.route.SeqStringCodec), depending on the kind of `QueryParam` that is being constructed. See [Codecs](codecs.md) for more on the codec types.

```scala mdoc:silent
val param = QueryParam.one[Int]("id") // Looks for StringCodec
```


### Query Parameter Semantics

Query parameter semantics can be quite complex. There are four cases to consider:

1. A parameter exists under the given name and the associated value can be decoded.
2. A parameter exists under the given name and the associated value cannot be decoded.
3. A parameter exists under the given name but there is no associated value.
4. No parameter exists under the given name.

The first case is the straightforward one where query parameter parsing always succeeds.

```scala mdoc:reset:invisible
import krop.all.*
```
```scala mdoc:silent
val required = QueryParam.one[Int]("id")
val optional = QueryParam.optional[Int]("id")
```
```scala mdoc
required.decode(Map("id" -> List("1")))
optional.decode(Map("id" -> List("1")))
```

In the second case both required and optional query parameters fail.

```scala mdoc
required.decode(Map("id" -> List("abc")))
optional.decode(Map("id" -> List("abc")))
```

A required parameter will fail in the third case, but an optional parameter will succeed with `None`.

```scala mdoc
required.decode(Map("id" -> List()))
optional.decode(Map("id" -> List()))
```

Similarly, a required parameter will fail in the fourth case but an optional parameter will succeed with `None`.

```scala mdoc
required.decode(Map())
optional.decode(Map())
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
intParam.decode("100")
```

A `Param.One[A]` can be lifted to a `Param.All[Seq[A]]` that uses the given
`Param.One` for every element in the `Seq`.

```scala mdoc:silent
val intParams = Param.all[Int]
```
```scala mdoc
intParams.encode(Seq(1, 2, 3))
```

The `separatedString` method can be used for a `Param.All` that constructs a `String`
containing elements separated by a separator. For example, to accumulate a
sub-path we could use the following.

```scala mdoc:silent
val subPath = Param.separatedString("/")
```
```scala mdoc
subPath.decode(Vector("assets", "css"))
subPath.encode("assets/css")
```

Finally, you can directly call the constructors for `Param.One` and `Param.All`.


### Param Names

`Params` have a `String` name. This is, by convention, some indication of the type written within angle brackets. For example `"<String>"` for a `Param[String]`.

```scala mdoc
Param.string.name
```

The name is mostly used in development mode, to output useful debugging information. You can change the name of a `Param` using the `withName` method. It's good practice to set the name whenever you create a new `Param`. For example, if deriving a new `Param` from an existing one you should consider changing the name.

```scala mdoc
// Bad, as the name doesn't reflect the underlying type.
intParam.name

// Better, as the name has been changed appropriately.
intParam.withName("<Int>").name
```
