# Codecs

Many tasks in Krop require converting to and from strings. For example, dealing with path segments, query parameters, or form submissions all requires converting values into strings or from strings, depending on exactly what we want to do. These conversions are handled by @:api(krop.route.StringCodec) and @:api(krop.route.SeqStringCodec).


## StringCodec

@:api(krop.route.StringCodec) decodes a `String` to a value and encodes a value as a `String`.


## SeqStringCodec

@:api(krop.route.SeqStringCodec) decodes a `Seq[String]` to a value and encodes a value as a `Seq[String]`.
