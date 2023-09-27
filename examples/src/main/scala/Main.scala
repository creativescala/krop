package examples

import krop.all.*

val application = Application.notFound

@main def notFound() =
  ServerBuilder.default.withApplication(application).run()
