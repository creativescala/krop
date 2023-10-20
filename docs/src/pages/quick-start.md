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


## Imports

To start using Krop you need to import the core Krop library. A single import is all you need.

```scala
import krop.all.{*, given}
```
