package controllers.captcha

import javax.inject.{ Inject, Singleton }
import play.api.data.Form
import play.api.libs.ws.WSClient
import play.api.mvc.{ AnyContent, Request }
import play.api.{ Configuration, Environment }

import scala.concurrent.{ ExecutionContext, Future }

import com.nappin.play.recaptcha._

/**
 * lib : https://github.com/chrisnappin/play-recaptcha/blob/release-2.3/docs/high-level-api.md
 */
@Singleton
class PlayRecaptchaVerifier @Inject() (
  configuration: Configuration,
  wsClient:      WSClient,
  env:           Environment,
  settings:      RecaptchaSettings
) extends RecaptchaVerifier(
    settings,
    new ParserWithHostName(configuration.get[String]("application.host").replaceAll(":9000", "")),
    wsClient
  ) {
  private[this] implicit val captcha_enabled = configuration.get[Boolean]("recaptcha.enabled")

  override def bindFromRequestAndVerify[T](form: Form[T])(implicit
    request:                                     Request[AnyContent],
    context:                                     ExecutionContext
  ): Future[Form[T]] = {
    if(captcha_enabled) {
      super.bindFromRequestAndVerify(form)
    } else {
      Future.successful(form.bindFromRequest())
    }
  }

  override def verifyV2(response: String, remoteIp: String)(implicit
    context:                      ExecutionContext
  ): Future[Either[Error, Success]] = {
    if(captcha_enabled) {
      super.verifyV2(response, remoteIp)
    } else {
      Future.successful(Right(Success()))
    }
  }
}
