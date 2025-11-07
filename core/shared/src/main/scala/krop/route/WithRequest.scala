package krop.route

trait WithRequest {
  def request: Request[?, ?, ?, ?]
}
