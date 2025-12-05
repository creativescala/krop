# Principles

Krop's goal is to make it delightful to build delightful web applications. This section unpacks those goals, describing in more detail what we're trying to do with Krop and how we're going to do it.


## Being Delightful

Making it delightful to build is another way of saying we care a lot about developer happiness and productivity. We want simple things (and we think a lot of the the web is simple) to be really simple. In fact we want working with Krop to fade into the background so that you can concentrate on what makes your application distinctive, not the HTTP plumbing. Here are some ways we're trying to achieve this.

- Scale down. Scala is proven in high scale situations. Every big project starts out small, so we want to make it really easy to get started. Building something should only be a few lines of code.

- Code is easy to write. When designing APIs we make it easy to follow the types of code, and follow the IDE autocomplete. For example, where the API requires a `Request` the developer can just start typing `Request` and follow methods on the companion object. They don't need to know that they're actually working with a builder type to construct the final `Request`.

- Krop is featureful. If a user wants to do a common task it should be in Krop. We don't want them to have to chase down a dozen dependencies just to get some basic stuff. This doesn't mean we write everything ourselves though; that is too big a job. If an existing library does the job we bring it into Krop. That why, for example, Krop builds on top of http4s. There is no value is creating yet another HTTP model and web server.

- Establish conventions. It's a waste of time for the user to have to come up with their own directory structure and other conventions. We establish these so others don't have to think about them.

- Avoid boilerplate. We use a variety of tactics, including sensible defaults, code generation, and Scala 3's compile-time metaprogramming, so the user can avoid writing boilerplate.

- Imports are minimized. Users don't need to know the project structure inside and out to get stuff done. We use Scala 3's `export` feature to coaelesce imports into a few chosen areas.

- Support the run time experience. The types don't tell us everything, so make it easy to debug as well. For example, this is why in development mode we show why routes didn't match a given request.

- Write documentation. Explain to our users how Krop works.


## Delightful Applications

We want to make it possible to build applications that feel amazing. Scala has a long history of building web APIs, and Krop fully supports this, but we also want to make it easy to create rich user interfaces. We have two approaches: server-centric and client-centric.

Server-centric applications are ones where the server provides the majority of the interaction, with a small bit of Javascript where necessary. This is the approach taken by [Phoenix LiveView](https://hexdocs.pm/phoenix_live_view/Phoenix.LiveView.html) and [Hotwired](https://hotwired.dev/). Krop will fully support this approach.

Client-centric applications build the interactivity on the client. Part of Scala's secret sauce is our compiler: we can produce Javascript and WASM in addition to JVM bytecode and native code. We intend to leverage this to make it possible to create web applications running in the browser or on mobile with just Scala code. Longer-term we want to support local-first applications. 


## Be Functional 

We follow a functional paradigm, meaning we emphasize composition and reasoning. This means the user can trace the path of a request to a response without any hidden state. We don't, for example, inject routes into some hidden global state. If a user wants a route they add it to their application. Similarly we use compile-time, not run-time metaprogramming.
