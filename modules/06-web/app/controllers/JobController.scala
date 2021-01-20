package controllers.web

import domain._

import javax.inject._
import play.api.data.Form
import play.api.data.Forms._
import utils.TimeUtils

import zio.clock.currentDateTime

@Singleton
class JobController @Inject() () extends MainController {

  protected val criteriaSearchForm = Form(
    mapping(
      "method" -> optional(text).transform[Option[HttpMethod]](
        _.flatMap(HttpMethod.parse(_)),
        _.map(_.productPrefix)
      )
    )(JobSearchCriteria.apply)(JobSearchCriteria.unapply)
  )

  def list() = Action.zio { implicit request =>
    for {
      tableCriteria <- tableSearchForm.bindFromRequest()    ?| ()
      criteria      <- criteriaSearchForm.bindFromRequest() ?| ()
      server_time   <- currentDateTime                      ?| "can't get current date time"
    } yield {
      Ok(views.html.jobs.list(TimeUtils.toIso(server_time), criteria, tableCriteria))
    }
  }
}
