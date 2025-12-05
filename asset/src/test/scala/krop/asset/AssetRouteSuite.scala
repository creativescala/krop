package krop.asset

import cats.effect.Deferred
import cats.effect.IO
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path as Fs2Path
import munit.CatsEffectSuite
import krop.route.Path
import org.http4s.server.websocket.WebSocketBuilder

import scala.concurrent.duration.*
import krop.JvmBaseRuntime
import krop.JvmKropRuntime
import krop.JvmRuntime

class AssetRouteSuite extends CatsEffectSuite {
  val files = Files.forIO

  def makeFiles(
      base: Fs2Path,
      filesAndContent: List[(String, String)]
  ): IO[Unit] =
    Stream
      .emits(filesAndContent)
      .flatMap { case (fileName, content) =>
        val file = base / fileName
        Stream(content).through(files.writeUtf8(file))
      }
      .compile
      .drain

  test("AssetRoute.asset returns correct href") {
    val baseRuntime = JvmRuntime.base
    val path = Path / "assets"
    val resource =
      for {
        dir <- files.tempDirectory
        route = AssetRoute(path, dir.toString)
        _ <- makeFiles(dir, List("a.txt" -> "ocelittle")).toResource
        handler <- route.build(baseRuntime)
        builder <- WebSocketBuilder[IO].toResource
        runtime <- baseRuntime.buildResources.map(r =>
          JvmKropRuntime(baseRuntime, r, builder)
        )
      } yield assertEquals(
        route.asset("a.txt")(using runtime),
        s"${path.pathTo(EmptyTuple)}/a-${"ocelittle".md5Hex}.txt"
      )

    resource.use_
  }
}
