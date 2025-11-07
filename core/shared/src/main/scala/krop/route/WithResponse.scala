package krop.route

trait WithResponse {
  def response: Response[?, ?]
}
