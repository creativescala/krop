package krop.sqlite

import cats.effect.IO
import cats.effect.Resource
import com.augustnagro.magnum.magcats.Transactor
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource

final class Sqlite private (filename: String, config: SQLiteConfig) {
  def create: Resource[IO, Transactor[IO]] = {
    val dataSource = SQLiteDataSource(config)
    dataSource.setUrl(s"jdbc:sqlite:./${filename}")
    Transactor[IO](dataSource).toResource
  }
}
object Sqlite {
  def create(filename: String): Resource[IO, Transactor[IO]] =
    Sqlite(filename, SQLiteConfig()).create
}
