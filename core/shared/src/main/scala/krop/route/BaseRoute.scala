package krop.route

/** A BaseRoute is just a marker indicating that something is a route, and has a
  * Request and a Response. Subtypes of BaseRoute, such as Route, are more
  * useful for day-to-day applications.
  */
trait BaseRoute {
  def request: Request[?, ?, ?, ?]
  def response: Response[?, ?]
}
