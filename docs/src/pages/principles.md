# Principles

The goal of Krop is to make building web services and applications easy. There are many existing web toolkits, so this section lays out the principles that guide the development of Krop and shows how it differs from other toolkits.

## Simple Things Are Easy

Above all, Krop aims to make it really really easy to create web applications. It should be trivial to create a simple website that consists of a few pages and some interactive parts. It should be equally easy to create a web service that responds to a few end points.

There are several implications of this focus on productivity:

- Code is easy to write. When designing APIs we make it easy to follow the types of code, and follow the IDE autocomplete. For example, where the API requires a `Request` the developer can just start typing `Request.`. They don't need to know that they're actually working with a builder type to construct the final `Request`.

- Krop is featureful. If a user wants to do a common task it should be in Krop. We don't want them to have to chase down a dozen dependencies just to get some basic stuff. This doesn't mean we write everything ourselves though; that is too big a job. If an existing library does the job we bring it into Krop. That why, for example, Krop builds on top of http4s. There is no value is creating yet another HTTP model and web server.

- Avoid boilerplate. We use a variety of tactics, including sensible defaults, code generation, and Scala 3's compile-time metaprogramming, so the user can avoid writing boilerplate.

- Imports are minimized. Users don't need to know the project structure inside and out to get stuff done. We use Scala 3's `export` feature to coaelesce imports into a few chosen areas.

## Be Functional

We follow a functional paradigm, meaning we emphasize composition and reasoning. This means the user can trace the path of a request to a response without any hidden state. We don't, for example, inject routes into some hidden global state. If a user wants a route they add it to their application. Similarly we use compile-time, not run-time metaprogramming.


## An Amazing Developer Experience

Krop wants to address the whole lifecycle of creating web applications. This means not only making it easy to write code, but also:

- supporting the runtime experience. The types don't tell us everything, so make it easy to debug as well. For example, this is why in development mode we show why routes didn't match a given request.

- writing documentation. Like the rest of the project our documentation is a continual work-in-progress but we do our best to explain to our users how Krop works.
