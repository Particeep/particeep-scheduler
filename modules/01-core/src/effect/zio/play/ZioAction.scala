package effect.zio.play

import effect.Fail
import play.api.Logging
import play.api.mvc._
import play.api.mvc.{ Action, ActionBuilder, BodyParser, Result }
import utils.StringUtils

import scala.util.control.NonFatal

import zio.{ Runtime, ZIO }

trait ZioAction[Config] extends Logging {

  protected def runtime: Runtime[Config]

  implicit class ActionBuilderOps[+R[_], B](actionBuilder: ActionBuilder[R, B]) {

    def zio(zioActionBody: R[B] => ZIO[Config, Fail, Result]): Action[B] = actionBuilder.async { request =>
      runtime.unsafeRunToFuture(
        appEffectToResult(zioActionBody(request))
      )
    }

    def zio[A](
      bp:            BodyParser[A]
    )(
      zioActionBody: R[A] => ZIO[Config, Fail, Result]
    ): Action[A] = actionBuilder(bp).async { request =>
      val result = runtime.unsafeRunToFuture(
        appEffectToResult(zioActionBody(request))
      )

      result.recover {
        case NonFatal(throwable) => {
          fail2result(Fail("Unknown exception during execution"), Some(throwable))
        }
      }(scala.concurrent.ExecutionContext.global)

      result
    }

    private[this] def appEffectToResult[E](effect: ZIO[Config, Fail, Result]): ZIO[Config, Throwable, Result] = {
      effect
        .either
        .map {
          case Right(result) => Right(result)
          case Left(fail)    => {
            fail
              .getRootException()
              .map {
                case NonFatal(t) => Right(fail2result(fail, Some(t)))
                case fatal       => Left(fatal)
              }.getOrElse {
                Right(fail2result(fail, None))
              }
          }
        }
        .absolve
    }

    private[this] def fail2result(fail: Fail, maybe_non_fatal: Option[Throwable]): Result = {
      fail match {
        case fail_with_result: FailWithResult => fail_with_result.result
        case fail: Fail                       => {
          val code = StringUtils.randomAlphanumericString(8)
          maybe_non_fatal
            .map(t => logger.error(s"[#$code]${fail.userMessage()}", t))
            .getOrElse(logger.error(s"[#$code]${fail.userMessage()}"))
          Results.BadRequest(s"[#$code]${fail.message}")
        }
      }
    }
  }
}
