# Static responses

Krop provides several Response constructors for serving static content. These differ in where the content is loaded from and in the type of value the route handler must return.

## StaticResource

StaticResource responds with a file loaded from the application’s classpath (resources). Files are resolved using the JVM classloader.

The pathPrefix is the resource directory, and the handler returns the remaining resource name.

```scala
val staticResourceHandleableRoute =
  Route(
    Request.get(Path / "kroptest" / "assets"),
    Response.staticResource("/kroptest/assets/")
  ).handle(() => "create.html")
```

In this example, a request to /kroptest/assets will respond with the resource
/kroptest/assets/create.html.

Only resources available on the classpath can be served. Files added to the filesystem at runtime are not visible.

## StaticFile

StaticFile responds with a single file loaded from the filesystem.

The file path is fixed when the response is defined, and no handler input is required.

```scala
val staticFileRoute = 
  Route(
    Request.get(Path / "kroptest" / "assets" / "create.html"),
    Response.staticFile("resources/kroptest/assets/create.html")
  ).passthrough
```


In this example, a request to /kroptest/assets/create.html will read the file
resources/kroptest/assets/create.html from disk.

## StaticDirectory

StaticDirectory responds with files loaded from a filesystem directory. The handler determines which file inside the directory is served.

The pathPrefix parameter specifies the base directory, and the handler returns a relative Fs2Path within that directory.

```scala
val staticDirectoryRoute =
  Route(
    Request.get(Path / "kroptest" / "assets2" / Param.string),
    Response.StaticDirectory(Fs2Path("resources/kroptest/assets"))
  ).handle(Fs2Path(_))
```

In this example, a request to /kroptest/assets2/create.html will read the file
resources/kroptest/assets/create.html from disk.