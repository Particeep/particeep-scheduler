package test.security

import org.scalatestplus.play.PlaySpec
import security.hmac._
import utils.StringUtils
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import effect.Fail
import zio._

class HmacSecurityWithCustomVerifierTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
    with HmacSecurityHelper {

  "HmacSecurityWithCustomVerifier" should {
    "compute authorization header with empty body" in {
      val now      = "2019-04-09T13:32:03Z"
      val security = new HmacCoreSecurity(HmacSecurityConfig("a_key", "a_secret", time_windows = 3600000))
      val req      = HmacSecurityRequest("GET", None, None, now)
      val auth     = security.authorization_header(req)

      auth must startWith("HMAC a_key:")
      auth mustBe "HMAC a_key:OTMyZTkzMDU4NjM5MWJiNWJmOGM4ZjliMDkzYjY4MjlhOTFhYjVjMQ=="

      verify_security(security, auth, req) mustBe true
    }

    "compute authorization header with right value and a custom validator" in {
      def user_id_validator(req: HmacSecurityRequest): ZIO[Any, Fail, Boolean] = ZIO.succeed {
        req.custom_headers
          .filter(_._1 == "user_id")
          .map(x => StringUtils.isUuid(x._2))
          .reduceOption(_ && _)
          .getOrElse(
            false
          )
      }

      val now      = "2019-04-09T13:32:04Z"
      val security = new HmacCoreSecurity(HmacSecurityConfig("a_key", "a_secret", time_windows = 3600000))
        .withVerifier(
          user_id_validator
        )
      val req      = HmacSecurityRequest(
        "GET",
        None,
        None,
        now,
        custom_headers = List("user_id" -> "8a6864bd-306c-455f-813a-eeff577fd799")
      )

      val auth = security.authorization_header(req)
      auth must startWith("HMAC a_key:")
      auth mustBe "HMAC a_key:YzgyZDZlZGUxNDA2MzA3NjNjOTYyZmM2M2E1ZGJiZjRhZjQ3OGEyNg=="

      verify_security(security, auth, req) mustBe true
    }

    "compute authorization header with wrong value and a custom validator" in {
      def user_id_validator(req: HmacSecurityRequest): ZIO[Any, Fail, Boolean] = ZIO.succeed {
        req.custom_headers
          .filter(_._1 == "user_id")
          .map(x => StringUtils.isUuid(x._2))
          .reduceOption(_ && _)
          .getOrElse(
            false
          )
      }
      val now      = "2019-04-09T13:32:04Z"
      val security = new HmacCoreSecurity(HmacSecurityConfig("a_key", "a_secret", time_windows = 3600000)).withVerifier(
        user_id_validator
      )
      val req      = HmacSecurityRequest("GET", None, None, now, custom_headers = List("user_id" -> "1234"))

      val auth = security.authorization_header(req)
      auth must startWith("HMAC a_key:")

      auth mustBe "HMAC a_key:MmQ4ZWYwZmYxYTQ1ZDEzOWJkNGU3NjRjYjhkODQ2MzUxNDg0M2U4ZA=="

      verify_security(security, auth, req) mustBe false
    }
  }
}
