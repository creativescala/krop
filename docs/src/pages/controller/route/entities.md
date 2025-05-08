# Entities

In HTTP, the term "entity" refers to the body of a request. For example, a JSON request will have JSON data in the entity. An @:api(krop.route.Entity) in Krop is responsible for converting an HTTP entity into a Scala type, and converting a Scala type into an HTTP entity. @:api(krop.route.Entity) is therefore a type of [codec](codecs.md). 

As well an decoding and encoding data, an @:api(krop.route.Entity) also specifies the [Content-Type][content-type] is supports for decoding and encoding. It is usually the case that the decoding Content-Type is more permissive than the encoding Content-Type. For example `Entity.text` will decode `text/*` but encodes `text/plain`.

The @:api(krop.route.Entity) type is defined as `Entity[D, E]`, where `D` is the type of value that will be decoded from an HTTP request, and `E` is the type of values that will be encoded in a response. Most of the time `D` and `E` are the same, and the entity is called an @:api(krop.route.InvariantEntity). Occassionally, however, they differ. For example, the [ScalaTags][scalatags] entity decodes a `String` but encodes ScalaTags' data structure. This is because there is no parser from `String` to ScalaTags, so there is no way to parse an HTTP request with an HTML entity into ScalaTags.


## Simple Entities

Simple entities decode and encode Scala types without an intermediaries. You should check the @:api(krop.route.Entity$) companion object for the full list of supported entities, but here are some of the most commonly used:

- `Entity.json` is an @:api(krop.route.InvariantEntity) for [Circe's][circe] `JSON` type, decoding and encoding Content-Type `application/json`.
- `Entity.html` is an @:api(krop.route.InvariantEntity) for `String`, decoding and encoding Content-Type `text/html`.
- `Entity.scalatags` is an @:api(krop.route.Entity) decoding `String` and encoding [Scalatag's][scalatags] `TypedTag[String]` type, with Content-Type `text/html`.
- `Entity.text` is an @:api(krop.route.InvariantEntity) for `String`, decoding Content-Type `text/*` and encoding Content-Type `text/plain`.
- `Entity.twirl` is an @:api(krop.route.Entity) decoding `String` and encoding [Twirl's][twirl] `Html` type, with Content-Type `text/html`.


## JSON

Krop, by default, uses [Circe][circe] for JSON decoding and encoding. If you have a type `A` that you want to decode and encode as JSON, assuming you have created given instances of Circe's `Decoder` and `Encoder` type for `A`, creating an @:api(krop.route.InvariantEntity) is as simple as calling
`Entity.jsonOf[A]`.

Here's a quick example, using Circe's semi-automatic generic derivation to create the `Decoder` and `Encoder` types.

```scala mdoc:silent
import io.circe.{Decoder, Encoder}
import krop.all.*

final case class Cat(name: String) derives Decoder, Encoder

val jsonEntity = Entity.jsonOf[Cat]
```


## Forms

Decoding and encoding of form data delegates to a @:api(krop.route.FormCodec) given instance. The usual way to create such an instance if with generic derivation. Here is an example, showing both generic derivation of the `FormCodec` instance and creation of an @:api(krop.route.Entity) using `Entity.formOf`.

```scala mdoc:silent
final case class Dog(name: String) derives FormCodec

val formEntity = Entity.formOf[Dog]
```


[circe]: https://circe.github.io/circe/
[content-type]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Type
[scalatags]: https://github.com/com-lihaoyi/scalatags
[twirl]: https://index.scala-lang.org/playframework/twirl
