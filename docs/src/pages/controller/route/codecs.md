# Codecs

Many tasks in Krop require converting to and from some external representation. For example, dealing with path segments, query parameters, or form submissions all requires converting values to and from strings. Types that handle these conversions are called codecs, which is short for "coder and decoder". 

Some codecs, such as [`Entity`](entities.md), have their own documentation. This section documents @:api(krop.route.StringCodec) and @:api(krop.route.SeqStringCodec). They are usually used as `given` values in constructing @:api(krop.route.Param), @:api(krop.route.Query), and @:api(krop.route.FormCodec) values, so the developer typically doesn't work with them directly if a predefined instance already exists for the types they are using.


## StringCodec

@:api(krop.route.StringCodec) decodes a `String` to a value and encodes a value as a `String`.


## SeqStringCodec

@:api(krop.route.SeqStringCodec) decodes a `Seq[String]` to a value and encodes a value as a `Seq[String]`.
