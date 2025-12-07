# Assets

Asset routes in Krop provide special support for serving assets. Assets are files, such as CSS stylesheets, that are part of your web application and not written in Scala. An asset route does two things:

1. It monitors a directory of files, calculating a hash of each file in the directory and updating that hash whenever it changes.

2. It constructs names for asset files that include the hash of the file. The means each version of the file is served with a unique file name, preventing web browsers from caching and using out-dated versions of assets.


## Using Assets

If you aren't using the [project template](../../quick-start.md) you will need to add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "org.creativescala" %% "krop-asset" % "@VERSION@"
```


## Creating an Asset Route

An @:api(krop.asset.AssetRoute) is constructed by giving it a @:api(krop.route.Path) under which to serve assets, and a `String` specifying a directory to monitor. So, for example, if we want to serve assets under the path `/assets`, and those assets live under the directory `resources/myapp/assets` (relative the project root directory) we would create an `AssetRoute` as

```scala mdoc:silent
import krop.all.*
import krop.asset.AssetRoute

val assets = AssetRoute(Path / "assets", "resources/myapp/assets")
```

Note we need to `import krop.asset.AssetRoute`; this not part of the core library we import with `krop.all.*`.

An asset route is also a handler, which we add to our application in the usual way. Code like the following will do.

```scala
assets.orElse(theApplication)
```


## Linking to an Asset

Call the `asset` method on an asset route when you want to link to an asset, for example in a template. Pass the method the path to the asset, relative to the directory you passed the asset route on construction.

For example, if we have a file `resources/myapp/assets/css/myapp.css` we would call

```scala 
assets.asset("css/myapp.css")
// res: String = /assets/css/myapp-1234.css
```

Notice the result includes the `Path` we created the asset route with. It also includes the value of a hash of the file (in the example replaced with `1234` for simplicity.) This value changes every time the file changes, and thus prevents the browser from using a stale cached copy.

The `asset` method requires a given `KropRuntime` value, which is available to all handlers when they handle a request.
