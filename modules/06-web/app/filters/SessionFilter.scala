package filters.web

import domain.User

import akka.stream.Materializer
import controllers.web.SecurityConstant
import javax.inject._
import play.api.Configuration
import play.api.mvc.Results.Redirect
import play.api.mvc._

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import utils.TimeUtils

/**
 * BlackListCookieFilter is a filter that allow logout to disable session totally
 *
 * This require a link to a storage service so that not decentralized auth anymore but we can't do better right now
 */
@Singleton
class SessionFilter @Inject() (val mat: Materializer, ec: ExecutionContext, config: Configuration) extends Filter {

  private[this] val white_list = List(
    "/",
    "/ping",
    "/login"
  )

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if(isOnWhiteList(requestHeader.path)) {
      nextFilter(requestHeader)
    } else {
      extract_user_from_session(requestHeader) match {
        case Some(user) => nextFilter(add_attributes_to_req(requestHeader, user))
        case None       => Future.successful(on_error)
      }
    }
  }

  def isOnWhiteList(path: String): Boolean = {
    white_list.contains(path) || path.startsWith("/assets/")
  }

  val on_error = Redirect(controllers.web.routes.AuthenticationController.login()).withNewSession

  private[this] def extract_user_from_session(requestHeader: RequestHeader): Option[User] = {
    requestHeader.session
      .get(SecurityConstant.USER_EMAIL)
      .filter(_ => !has_expired(requestHeader.session))
      .map(email => User(email, ""))
  }

  private[this] def add_attributes_to_req(requestHeader: RequestHeader, user: User): RequestHeader = {
    requestHeader.addAttr(SecurityConstant.User, user)
  }

  private[this] val expiration_after_minutes = config.get[Long]("application.session.expire.after_minutes")
  private[this] val expire_check_is_active   = expiration_after_minutes != -1
  private[this] def has_expired(session: Session): Boolean = {
    if(!expire_check_is_active) {
      return false
    }

    session.get("expire_at")
      .flatMap(d => Try(d.toLong).toOption)
      .map(date => OffsetDateTime.ofInstant(Instant.ofEpochSecond(date), ZoneOffset.UTC))
      .map(_.isBefore(TimeUtils.now())) getOrElse (false)
  }
}
