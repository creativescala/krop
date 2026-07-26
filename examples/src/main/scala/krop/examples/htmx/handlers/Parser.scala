package krop.examples.htmx.handlers

import org.http4s.headers.Cookie

object Parser:
  extension (cookie: Cookie)
    def getToken: Option[String] =
      cookie.values.collectFirst:
        case rq if rq.name == "token" => rq.content
