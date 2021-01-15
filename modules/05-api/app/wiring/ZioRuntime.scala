package wiring

import repository._

import services._

import effect.zio.PlatformAppSpecific
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Logging }

import zio.internal.Platform
import zio.{ Cause, Has, Runtime, ZLayer }

@Singleton
class ZioRuntime @Inject() (
  ws:               WSClient,
  configuration:    Configuration,
  dbConfigProvider: DatabaseConfigProvider
) {

  lazy val live: ZLayer[Any, Nothing, ZioRuntime.AppContext] =
    ZioRuntime.buildLiveLayer(ws, configuration, dbConfigProvider)

  lazy val runtime: Runtime[ZioRuntime.AppContext] =
    Runtime.default.unsafeRun(live.toRuntime(ZioRuntime.platform).useNow)
}

object ZioRuntime extends Logging {

  def buildLiveLayer(
    ws:               WSClient,
    configuration:    Configuration,
    dbConfigProvider: DatabaseConfigProvider
  ) = {
    PlatformAppSpecific.ZEnv.live andTo
      ZLayer.succeed(configuration) andTo
      ZLayer.succeed(dbConfigProvider) andTo
      ZLayer.succeed(ws) andTo
      ExecutionRepository.in_db andTo
      JobRepository.in_db andTo
      SchedulerService.live andTo
      RunningJobService.live andTo
      JobService.live
  }

  type AppContext =
    PlatformAppSpecific.ZEnv
      with Has[WSClient]
      with Has[Configuration]
      with Has[JobService]
      with Has[SchedulerService]
      with Has[ExecutionRepository]
      with Has[JobRepository]
      with Has[RunningJobService]

  lazy val platform = Platform.default
    .withReportFailure { cause: Cause[Any] =>
      if(cause.died) {
        logger.error(cause.prettyPrint)
      }
    }
    .withReportFatal { t: Throwable =>
      logger.error("Fatal Error in ZIO", t)
      try {
        java.lang.System.exit(-1)
        throw t
      } catch { case _: Throwable => throw t }
    }
}
