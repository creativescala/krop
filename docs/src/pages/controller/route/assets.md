# Assets

Asset routes in Krop provide special support for serving assets. Assets are files, such as CSS stylesheets, that are part of your web application and not written in Scala. An asset route does two things:

1. It monitors a directory of files, calculating a hash of each file in the directory and updating that hash whenever it changes.

2. It constructs names for asset files that include the hash of the file. The means each version of the file is served with a unique file name, preventing web browsers from caching and using out-dated versions of assets.


## Using Assets

If you aren't using the [project template](../../quick-start.md) you will need to add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "org.creativescala" %% "krop-asset" % "@VERSION@"
```


## Asset Routes

An @:api(krop.asset.AssetRoute) is constructed by giving it a @:api(krop.route.Path) under which to serve assets, and a `String` specifying a directory to monitor. So, for example, if we want to serve assets under the path `/assets`, and those assets live under the directory `resources/myapp/assets` (relative the project root directory) we would create an `AssetRoute` as

```scala
import krop.all.*
import krop.asset.AssetRoute

val assets = AssetRoute(Path / "assets", "resources/myapp/assets")
```
