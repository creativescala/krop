# Quick Start

In this section we'll go through a quick example that illustrates all the major features of Krop.


## Project Template

To get started, create a project from the Krop project template.

```sh
sbt new creativescala/krop-fullstack.g8
```

This will prompt you for a few values then create a project for you. 
Change into the directory it created, and run sbt. Now within sbt run the command

```sh
backend / run
```

This will start the server. Visit `http://localhost:8080/` to see the masterpiece you have just created.


## Manual Setup

If you don't want to use the project template above, the steps for using Krop are:

1. Add the Krop dependency to your project's dependencies.

   ```scala
   libraryDependencies += "org.creativescala" %% "krop-core" % "@VERSION@"
   ```

2. Fork when running the server, otherwise the server's socket will not released when the server finishes.

   ```scala
   run / fork := true 
   ```
3. (Optional) Configure Krop so it runs in development mode.

   ```scala
   run / javaOptions += "-Dkrop.mode=development"
   ```
   
4. (Optional) Add a logging backend.

   ```scala
   libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.0 % Runtime 
   ```


## Imports

To start using Krop you need to import the core Krop library. A single import is all you need.

```scala
import krop.all.{*, given}
```
