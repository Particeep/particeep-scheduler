package controllers.web

import javax.inject._

@Singleton
class ApplicationController @Inject() () extends MainController {

  def index() = Action {
    Redirect(controllers.web.routes.JobController.list())
  }
}
