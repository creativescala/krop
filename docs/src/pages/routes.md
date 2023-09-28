# Routes


## Paths

A @:api(krop.route.Path) represents a pattern to match against the path
 component of the URI of a request.

 `Paths` are created starting with `Path.root` and then calling the `/`
 method to add segments to the pattern. For example

 ```scala mdoc:silent
 Path.root / "user" / "create"
 ```

 matches a request with the path `/user/create`.

 Use a @:api(krop.route.Param) to capture part of the path for later processing.
 For example

 ```scala mdoc:silent
 Path.root / "user" / Param.int / "view"
 ```

 matches `/user/<id>/view`, where `<id>` is an `Int`, and makes the `Int`
 value available to the request handler.

 A `Path` will fail to match if the URI's path has more segments than the
 `Path` matches. So `Path.root / "user" / "create"` will not match
 "/user/create/1234". Use `Segment.all` to match and ignore all the segments
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
