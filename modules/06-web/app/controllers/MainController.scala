package controllers.web

import domain.TableSearch

import controllers.PlayHelper
import effect.zio.play.ZioController
import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, Lang }
import play.api.mvc._
import web.wiring.ZioRuntime

trait MainController
  extends InjectedController
    with I18nSupport
    with ZioController[ZioRuntime.AppContext]
    with PlayHelper {

  @Inject() private[this] var _runtime: ZioRuntime = _
  lazy val runtime                                 = _runtime.runtime
  lazy val layer                                   = _runtime.live

  implicit def request2lang(implicit request: Request[_]): Lang = {
    request.lang
  }

  protected val tableSearchForm = Form(
    mapping(
      "global_search" -> optional(text),
      "sort_by"       -> default(optional(text), Some("created_at")),
      "order_by"      -> default(optional(text), Some("desc")),
      "offset"        -> default(optional(number), Some(0)),
      "limit"         -> default(optional(number), Some(30))
    )(TableSearch.apply)(TableSearch.unapply)
  )
}
