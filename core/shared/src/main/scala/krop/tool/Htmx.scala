package krop.tool

import scalatags.Text.all.attr
import scalatags.Text.Attr

object Htmx {
  val hxBoost: Attr = attr("hx-boost")
  val hxGet: Attr = attr("hx-get")
  val hxPost: Attr = attr("hx-post")
  val hxTarget: Attr = attr("hx-target")
}
