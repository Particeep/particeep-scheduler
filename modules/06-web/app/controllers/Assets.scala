package controllers.web

import controllers.AssetsMetadata
import javax.inject._
import play.api.http.HttpErrorHandler

class Assets @Inject() (errorHandler: HttpErrorHandler, meta: AssetsMetadata)
  extends controllers.AssetsBuilder(errorHandler, meta)
