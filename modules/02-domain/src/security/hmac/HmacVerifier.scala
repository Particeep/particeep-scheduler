package security.hmac

import effect.Fail
import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.mvc.RequestHeader
import security.hmac.verifier.NonceVerifier

import scala.concurrent.ExecutionContext

import zio._

@Singleton
class HmacVerifier @Inject() (configuration: Configuration, cache: AsyncCacheApi, ec: ExecutionContext) {

  lazy val config = HmacSecurity.parse_config(configuration)

  val nonce_verifier = new NonceVerifier(cache)(ec)

  def verify(req: RequestHeader): ZIO[Any, Fail, Boolean] = {
    verify(req, config.key, config.secret)
  }

  def verify(
    req:    RequestHeader,
    key:    String,
    secret: String
  ): ZIO[Any, Fail, Boolean] = {
    req.headers.get("Authorization")
      .map { auth =>
        val custom_headers = req.headers.toSimpleMap
          .filter(h => config.custom_headers.exists(_.equalsIgnoreCase(h._1)))
          .toList

        new HmacCoreSecurity(config.copy(key = key, secret = secret))
          .withVerifier(nonce_verifier.verify)
          .verify(auth, HmacSecurity.requestHeader2HmacRequest(req, custom_headers))(ec)
      }
      .getOrElse(ZIO.fail(Fail("Can't find Authorization header")))
  }
}
