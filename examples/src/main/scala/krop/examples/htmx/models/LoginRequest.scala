package krop.examples.htmx.models

import io.circe.*

final case class LoginRequest(
    username: String,
    password: String
) derives Decoder,
      Encoder
