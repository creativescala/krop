package krop.route

import cats.syntax.all.*

/** A SeqStringCodec encodes a value as a sequence of String, and decodes a
  * value from a sequence of String. SeqStringCodecs are used for handling:
  *
  *   - path segments;
  *   - query parameters; and
  *   - form submission.
  */
trait SeqStringCodec[A] {

  /** A short description of this codec. By convention the name of the type this
    * codec encodes.
    */
  def name: String
  def decode(value: Seq[String]): Either[SeqStringDecodeFailure, A]
  def encode(value: A): Seq[String]
}
object SeqStringCodec {

  /** A [[SeqStringCodec]] that passes through its input unchanged. */
  given seqString: SeqStringCodec[Seq[String]] =
    new SeqStringCodec[Seq[String]] {
      val name: String = "Seq[String]"

      def decode(
          value: Seq[String]
      ): Either[SeqStringDecodeFailure, Seq[String]] =
        Right(value)

      def encode(value: Seq[String]): Seq[String] = value
    }

  /** Constructs a [[SeqStringCodec]] from a given [[StringCodec]]. Decoding
    * fails if no values are available.
    */
  given fromStringCodec[A](using codec: StringCodec[A]): SeqStringCodec[A] =
    new SeqStringCodec[A] {
      val name: String = codec.name

      def decode(value: Seq[String]): Either[SeqStringDecodeFailure, A] =
        value.headOption
          .map(
            codec
              .decode(_)
              .leftMap(SeqStringDecodeFailure.fromStringDecodeFailure)
          )
          .getOrElse(Left(SeqStringDecodeFailure(value, codec.name)))

      def encode(value: A): Seq[String] =
        Seq(codec.encode(value))
    }

  /** Constructs a [[SeqStringCodec]] from a given [[StringCodec]]. Decoding
    * returns None if no values are available; otherwise the values is Some of
    * the decoded values.
    */
  given optional[A](using codec: StringCodec[A]): SeqStringCodec[Option[A]] =
    new SeqStringCodec[Option[A]] {
      val name: String = codec.name

      def decode(
          value: Seq[String]
      ): Either[SeqStringDecodeFailure, Option[A]] =
        value.headOption
          .map(
            codec
              .decode(_)
              .leftMap(SeqStringDecodeFailure.fromStringDecodeFailure)
              .map(_.some)
          )
          .getOrElse(Right(None))

      def encode(value: Option[A]): Seq[String] =
        value.map(codec.encode).toSeq
    }
}

/** Indicates that decoding a sequence of string failed.
  *
  * @param input:
  *   The string for which decoding was attempted
  * @param expected:
  *   A description of what was expected. By convention this is the type name.
  */
final case class SeqStringDecodeFailure(input: Seq[String], expected: String)
object SeqStringDecodeFailure {
  def fromStringDecodeFailure(
      failure: StringDecodeFailure
  ): SeqStringDecodeFailure =
    SeqStringDecodeFailure(Seq(failure.input), failure.expected)
}
