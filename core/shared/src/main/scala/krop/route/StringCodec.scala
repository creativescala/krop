package krop.route

/** A StringCodec encodes a value as a String, and decodes a value from a
  * String. StringCodecs are used for handling:
  *
  *   - path segments;
  *   - query parameters; and
  *   - form submission.
  */
trait StringCodec[A] {

  /** A short description of this codec. By convention the name of the type this
    * codec encodes.
    */
  def name: String
  def decode(value: String): Either[StringDecodeFailure, A]
  def encode(value: A): String
}
object StringCodec {
  given int: StringCodec[Int] =
    new StringCodec[Int] {
      val name: String = "Int"

      def decode(value: String): Either[StringDecodeFailure, Int] =
        value.toIntOption.toRight(StringDecodeFailure(value, name))

      def encode(value: Int): String = value.toString
    }

  given string: StringCodec[String] =
    new StringCodec[String] {
      val name: String = "String"

      def decode(value: String): Either[StringDecodeFailure, String] =
        Right(value)

      def encode(value: String): String = value
    }
}

/** Indicates that decoding a string failed.
  *
  * @param input:
  *   The string for which decoding was attempted
  * @param expected:
  *   A description of what was expected. By convention this is the type name.
  */
final case class StringDecodeFailure(input: String, expected: String)
