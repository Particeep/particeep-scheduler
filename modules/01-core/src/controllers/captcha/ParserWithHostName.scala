package controllers.captcha

import play.api.libs.json.JsValue

import com.nappin.play.recaptcha.{ Error, RecaptchaErrorCode, ResponseParser, Success }

class ParserWithHostName(domain: String) extends ResponseParser {

  override def parseV2Response(response: JsValue): Either[Error, Success] = {
    super.parseV2Response(response).flatMap(_ => verifyHostname(response))
  }

  private[this] def verifyHostname(response: JsValue): Either[Error, Success] = {
    (response \ "hostname").asOpt[String]
      .filter(_ == domain)
      .map(_ => Right(Success()))
      .getOrElse(Left(Error(RecaptchaErrorCode.apiError)))
  }
}
