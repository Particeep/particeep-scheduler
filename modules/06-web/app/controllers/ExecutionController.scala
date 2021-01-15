package controllers.web

import domain._

import javax.inject._
import play.api.data.Form
import play.api.data.Forms._

@Singleton
class ExecutionController @Inject() () extends MainController {

  protected val executionSearchForm = Form(
    mapping(
      "runned_after"  -> optional(offsetDate),
      "runned_before" -> optional(offsetDate),
      "status"        -> optional(number)
    )(ExecutionSearchCriteria.apply)(ExecutionSearchCriteria.unapply)
  )

  def list() = Action.zio { implicit request =>
    for {
      criteria      <- executionSearchForm.bindFromRequest() ?| ()
      tableCriteria <- tableSearchForm.bindFromRequest()     ?| ()
    } yield {
      Ok(views.html.execution.list(criteria, tableCriteria))
    }
  }
}
