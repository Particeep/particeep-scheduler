package web.wiring

import effect.zio.PlatformAppSpecific
import javax.inject.{ Inject, Singleton }
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Logging }

import zio.internal.Platform
import zio.{ Cause, Has, Runtime, ZLayer }

@Singleton
class ZioRuntime @Inject() (
  ws:            WSClient,
  configuration: Configuration
) {

  lazy val live: ZLayer[Any, Nothing, ZioRuntime.AppContext] =
    ZioRuntime.buildLiveLayer(configuration)

  lazy val runtime: Runtime[ZioRuntime.AppContext] =
    Runtime.default.unsafeRun(live.toRuntime(ZioRuntime.platform).useNow)
}

object ZioRuntime extends Logging {

  def buildLiveLayer(
    configuration: Configuration
  ) = {
    PlatformAppSpecific.ZEnv.live andTo
      ZLayer.succeed(configuration)
  }

  type AppContext =
    PlatformAppSpecific.ZEnv with Has[Configuration]

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
