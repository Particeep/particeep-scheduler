package test.operation

import domain._
import org.scalatestplus.play.PlaySpec
import operation.CredentialOperation

class CredentialOperationTest extends PlaySpec {

  val base_job = Job(
    method      = HttpMethod.GET,
    url         = new Url("http://localhost:9000/catalogue/api/product"),
    credentials = None,
    name        = "xx",
    start_time  = "now",
    frequency   = Frequency("once")
  )

  "CredentialOperation" should {

    "generate empty headers for request without credentials" in {
      CredentialOperation.security_headers(base_job) mustBe Map.empty
    }

    "generate headers to authenticate an HMAC request" in {

      val creds  = HmacCredential(
        api_key     = "1234",
        api_secret  = "5678",
        algo        = "HMAC",
        prefix      = Some("PLG"),
        time_window = Some(10),
        headers     = Some(List(
          "Broker_id:b_id_1",
          "Broker_user_id:b_u_id_1",
          "Info-End-User:IEU-particeep_scheduler",
          "Info-End-User-Ip:IP-scheduler.particeep.com",
          "X-Requested-With:X-scheduler.particeep.com",
          "Referer:R-scheduler.particeep.com",
          "user-agent:UA-play_ws_client",
          "nonce"
        ))
      )
      val job    = base_job.copy(credentials = Some(creds))
      val result = CredentialOperation.security_headers(job)

      val expected_result = Map(
        "user-agent"       -> "UA-play_ws_client",
        "Referer"          -> "R-scheduler.particeep.com",
        "Info-End-User-Ip" -> "IP-scheduler.particeep.com",
        "Broker_user_id"   -> "b_u_id_1",
        "X-Requested-With" -> "X-scheduler.particeep.com",
        "Broker_id"        -> "b_id_1",
        "Info-End-User"    -> "IEU-particeep_scheduler"
      )

      result.removedAll(List("Date", "nonce", "Authorization")) mustBe expected_result

      result.get("Date").isDefined mustBe true
      result.get("nonce").isDefined mustBe true
      result.get("Authorization").isDefined mustBe true
      result.get("Authorization").map(_.startsWith("PLG 1234:")) mustBe Some(true)
    }
  }
}
