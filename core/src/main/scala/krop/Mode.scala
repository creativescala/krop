package krop

/** Krop can run in one of two modes: development and production. In development
  * mode it shows output that is useful for debugging and otherwise inspecting
  * the running state. In production this output is hidden.
  *
  * The mode is set by the KROP_ENV environment variable. If it has the value of
  * "development" (without the quotes; any capitalization is fine) then the mode
  * is development. Otherwise it is production.
  *
  * The mode is determined when Krop starts.
  */
enum Mode {
  case Production
  case Development

  def isProduction: Boolean =
    this match {
      case Production  => true
      case Development => false
    }

  def isDevelopment: Boolean =
    this match {
      case Production  => false
      case Development => true
    }
}
object Mode {

  /** The name of the environment variable used to set the Krop mode. */
  val environmentVariable = "KROP_ENV"

  /** The mode in which Krop is running. */
  val mode: Mode = {
    val envVar = System.getenv(environmentVariable)
    if (envVar == null) Mode.Production
    else if (envVar.toLowerCase() == "development") Mode.Development
    else Mode.Production
  }
}
