package krop.examples.htmx.routes

import io.circe.Json
import krop.Types.TupleConcat
import krop.all.{Entity, Path, Request, Response, Route, *}
import krop.examples.htmx.models.LoginRequest
import org.http4s.Status as HttpStatus
import org.http4s.headers.*
import org.http4s.headers.Authorization

object Routes:
  val index =
    Route(
      Request.get(Path.root).extractHeader[`Cookie`],
      Response.ok(Entity.html)
    )

  val home =
    Route(
      Request.get(Path.root / "home").extractHeader[`Cookie`],
      Response.ok(Entity.html)
    )

  val register =
    Route(
      Request.get(Path.root / "register"),
      Response.ok(Entity.html)
    )

  val login = Route(
    Request
      .post(Path.root / "auth" / "login")
      .withEntity(Entity.jsonOf[LoginRequest]),
    Response
      .ok(Entity.html)
      .orElse(Response.status(HttpStatus.Forbidden, Entity.html))
      .orNotFound
  )

  val newUser = Route(
    Request
      .post(Path.root / "new_user")
      .withEntity(Entity.jsonOf[LoginRequest]),
    Response
      .status(HttpStatus.Created, Entity.html)
      .orElse(Response.status(HttpStatus.Conflict, Entity.html))
      .orNotFound
  )

  val logout = Route(
    Request
      .post(Path.root / "auth" / "logout")
      .extractHeader[Authorization],
    Response
      .ok(Entity.html)
      .orElse(Response.status(HttpStatus.Forbidden, Entity.html))
      .orNotFound
  )

  val assetRoute =
    Route(
      Request.get(Path.root / "asset" / Params.separatedString("/")),
      Response.staticResource("/asset/")
    )
end Routes
