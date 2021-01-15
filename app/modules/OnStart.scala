package modules

import services.JobService

import javax.inject._
import play.api.Configuration
import play.api.Logging
import utils.TimeUtils
import wiring.ZioRuntime

import scala.util.Try

import zio._
import zio.clock._

/**
 * OnStart code is executed in the constructor of this class
 */
@Singleton
class OnStart @Inject() (
  config:      Configuration,
  zio_runtime: ZioRuntime
) extends Logging {
  logger.info("--- App started ---")
  launchAllJob()
  logServerTime()

  private[this] def launchAllJob() = {
    Try {
      val effect = JobService.startAllJobs()
        .map { result =>
          logger.info(s"${result.length} jobs have been schedule")
        }
        .mapError { fail =>
          fail.getRootException() match {
            case Some(t) => logger.error(fail.userMessage(), t)
            case None    => logger.error(fail.userMessage())
          }
        }
      zio_runtime.runtime.unsafeRun(effect)
      ()
    }.recover {
      case t: Throwable => {
        logger.error("Error while starting all jobs", t)
      }
    }
  }

  private[this] def logServerTime() = zio_runtime.runtime.unsafeRun {
    for {
      time <- currentDateTime
      _    <- ZIO.succeed(logger.info("Server clock : " + TimeUtils.toIso(time)))
    } yield {
      ()
    }
  }
}
