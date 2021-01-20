package controllers.api

import domain._

import repository.ExecutionRepository

import javax.inject._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ExecutionController extends MainController with JsonParser {

  protected val executionSearchForm = Form(
    mapping(
      "runned_after"  -> optional(offsetDate),
      "runned_before" -> optional(offsetDate),
      "status"        -> optional(number)
    )(ExecutionSearchCriteria.apply)(ExecutionSearchCriteria.unapply)
  )

  def list() = Action.zio { implicit request =>
    for {
      criteria       <- executionSearchForm.bindFromRequest() ?| ()
      tableCriteria  <- tableSearchForm.bindFromRequest()     ?| ()
      execution_list <- ExecutionRepository.search(criteria, tableCriteria)
    } yield {
      Ok(Json.toJson(execution_list))
    }
  }
}
