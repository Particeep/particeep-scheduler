package filters.web

import akka.stream.Materializer
import controllers.PlayHelper
import controllers.web.SecurityConstant
import javax.inject._
import play.api.mvc.Results.{ Forbidden, Redirect }
import play.api.mvc._
import play.api.{ Configuration, Logging }
import utils.TimeUtils

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * This filter redirect to the login if the session as expired
 *
 * negative value of play.application.inactivity.seconds_allowed disable the filter
 */
@Singleton
class ExpireSessionFilter @Inject() (
  val mat:     Materializer,
  config:      Configuration
)(implicit ec: ExecutionContext)
  extends Filter
    with Logging {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if(isEnable(requestHeader) && hasSessionWithToken(requestHeader)) {
      if(isTokenExpired(requestHeader)) {
        onExpire(requestHeader)
      } else {
        renewSession(nextFilter)(requestHeader)
      }
    } else {
      nextFilter(requestHeader)
    }
  }

  private[this] def onExpire(requestHeader: RequestHeader): Future[Result] = {
    logger.debug("[ExpireSessionFilter] Session Expired")
    if(PlayHelper.is_ajax(requestHeader)) {
      Future.successful(Forbidden(SecurityConstant.authentication_error))
    } else {
      Future.successful(Redirect(controllers.web.routes.ApplicationController.index()).withNewSession)
    }
  }

  private[this] def renewSession(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader)
    : Future[Result] = {
    nextFilter(requestHeader).map { r =>
      r.withSession(
        r.session(requestHeader) + ("expire_at" -> TimeUtils.now().plusMinutes(
          expiration_after_minutes
        ).toInstant.toEpochMilli.toString)
      )
    }
  }

  private[this] def hasSessionWithToken(requestHeader: RequestHeader) = {
    requestHeader.session.get("expire_at").isDefined
  }

  // we filter only if expire_at is present in the token
  private[this] def isTokenExpired(requestHeader: RequestHeader): Boolean = {
    (for {
      expire_at <- requestHeader.session.get("expire_at")
    } yield {
      Try {
        expire_at.toLong < timestamp()
      }.getOrElse(true)
    }).getOrElse(false)
  }

  private[this] def timestamp(): Long = TimeUtils.now().toInstant.toEpochMilli

  private[this] val expiration_after_minutes = config.get[Long]("application.session.expire.after_minutes")
  private[this] val is_active                = expiration_after_minutes != -1
  private[this] def isEnable(header: RequestHeader): Boolean = is_active
}
