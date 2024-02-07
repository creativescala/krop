# Krop

A compositional web service library that is easy to use. Let a thousand websites bloom.

[Read more](https://creativescala.org/krop).


## Goals

This is an experiment to see if `http4s` can be made easy to use. The goal is to have an amazing developer experience while keeping the core practices of functional programming that are currently embodied by `http4s`: compositionality and abstraction.

Key points:

- Single import to access all functionality
- Default to `IO` and hide it's usage as far as possible. Tagless final is pointless ceremony for most applications.
- Wrap generic types like `Kleisli` so that only domain specific functionality is exposed.
- Default routing DSL that is discoverable (i.e. not using pattern matching). 
- Make very simple things, like creating mostly static sites, very easy.

As an example of what we're aiming for, take a look at [the Phoenix framework](https://www.phoenixframework.org/). Elixir is not a popular language but this framework has outstanding documentation and a well defined story for every common use case.


## Development

Use the `build` command in sbt to do everything (build code, run tests, format code, etc.)

Releases are automatically published whenever a tag like `x.y.z` is pushed to `main`.
