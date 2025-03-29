# Architecture

Krop uses a model-view-controller (MVC) architecture, which divides a web application into three components:

- [models](../model/README.md), which manages the data in the application;
- [views](../views.md), which is responsible for generating the user interface; and
- [controllers](../controller/README.md), which handles actions from the user interface and implements the application logic.


## Directory Structure

The standard directory structure for a Krop project is described below. It's similar to the standard structure for a Scala project, but it drops the `main/scala` directories that sbt inherits from Maven. This is a bit of unnecessary indirection that makes it harder to find the code and get stuff done, particularly when new to a project.

```
backend - All backend (server) code here
├── src
│   └── <package>
│       ├── Main.scala - the main entry point to your backend
│       ├── models
│       │   └── db - database models
│       └── views - template views using Twirl
├── resources - static files that are packaged with your application
└── test - tests 

frontend - All frontend (client) code here

shared - Code that is shared between backend and frontend
└── src
    └── <package>
        └── routes - your application's routes
```
