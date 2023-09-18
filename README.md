# Krop

A compositional web service library that is easy to use. [Read more](https://creativescala.org/krop).


## Goals

This is an experiment to see if `http4s` can be made easy to use. The goal is to have an amazing developer experience while keeping the core practices of functional programming that are currently embodied by `http4s`: compositionality and abstraction.

Key points:

- Single import to access all functionality
- No `IO` in types like `Request[IO, A]`. Default to `IO` and use opaque types to hide this. Tagless final is pointless ceremony for most applications.
- Use opaque types to narrow generic types like `Kleisli` so that only domain specific functionality is exposed.
- Default routing DSL that is discoverable (i.e. not using pattern matching). Will probably use an existing library like `endpoints4s` or `tapir` for this.
- Make very simple things, like creating mostly static sites, very easy.

As an example of what we're aiming for, take a look at [the Phoenix framework](https://www.phoenixframework.org/). Elixir is not a popular language but this framework has outstanding documentation and a well defined story for every common use case.
