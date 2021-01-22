package effect.zio

import effect.zio.sorus.ZioSorus
import effect.Fail
import utils.StringUtils
import _root_.play.api.Logging

package object play {

  trait ZioController[Config] extends ZioSorus with ZioPlayHelper with Logging {

    def log_fail(fail: Fail): Unit = {
      val code = StringUtils.randomAlphanumericString(8)
      fail
        .getRootException()
        .map(t => logger.error(s"[#$code]${fail.userMessage()}", t))
        .getOrElse(logger.error(s"[#$code]${fail.userMessage()}"))
    }

  }
}
