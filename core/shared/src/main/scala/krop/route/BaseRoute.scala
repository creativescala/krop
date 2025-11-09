package krop.route

/** A BaseRoute indicates that something is a route, and has a Request and a
  * Response. The types of the Request and Response are erased, so this is only
  * useful for runtime introspection. Other types keep more information and are
  * useful for other cases.
  */
trait BaseRoute {
  def request: Request[?, ?, ?, ?]
  def response: Response[?, ?]
}
