# SQLite

[SQLite][sqlite] is an in-process database that is becoming more popular for web applications due to the simplicity of running it. It's appropriate for applications that don't need to expand beyond a single web server, which covers many applications due to the speed of modern computers.
Krop provides SQLite integration via the [sqlite-jdbc][sqlite-jdbc] and [Magnum][magnum] projects.

## Using SQLite

Krop's integration lives in a separate artifact. To use it you'll need to add the following dependency to your `build.sbt`.

```scala
libraryDependencies += "org.creativescala" %% "krop-sqlite" % "@VERSION@"
```


## Creating a Database Connection

Connecting to a database is trivial, as SQLite only requires the name of the file that stores the database. If the file doesn't already exist it will be created. The code below shows how to connect to a database. This uses a default configuration that has been tuned to the needs of a typical web application.

```scala mdoc:silent
import cats.effect.{IO, Resource}
import krop.sqlite.{Sqlite, Transactor}

val dbFile = "./database.sqlite3"
val db: Resource[IO, Transactor] = Sqlite.create(dbFile)
```

If we wanted a custom configuration we could set one ourselves. If the example below we use an empty configuration, but we can call methods on the object returned by `empty` to customise the configuration.

```scala mdoc:silent
Sqlite.create(dbFile, Sqlite.config.empty)
```

The value returned by `Sqlite.create` is a Cats Effect [Resource][resource] containing a @:api(krop.sqlite.Transactor). This means that nothing actually happens until we `use` the `Resource`, with code like the following.

```scala
db.use { xa =>
  // Use the Transactor, xa, here
}
```

The @:api(krop.sqlite.Transactor) is a [Magnum][magnum] type to work with databases.


[sqlite]: https://sqlite.org/
[sqlite-jdbc]: https://github.com/xerial/sqlite-jdbc
[magnum]: https://github.com/augustnagro/magnum
[resource]: https://typelevel.org/cats-effect/docs/std/resource
