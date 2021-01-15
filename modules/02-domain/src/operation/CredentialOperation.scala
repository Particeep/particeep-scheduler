package operation

import domain._

import play.api.Configuration
import security.hmac._
import utils.{ StringUtils, TimeUtils }

object CredentialOperation {

  def security_headers(job: Job): Map[String, String] = {
    job.credentials match {
      case Some(creds) => security_headers(job, creds)
      case None        => Map.empty
    }
  }

  private[this] def security_headers(job: Job, credentials: HmacCredential): Map[String, String] = {
    val config          = buildHmacConfig(credentials)
    val dynamic_headers = Map(
      ("Date"  -> TimeUtils.now_iso()),
      ("nonce" -> StringUtils.randomAlphanumericString(40))
    )

    val all_header = credentials.headerMap() ++ dynamic_headers
    val hmac_req   = buildHmacReq(job, config, all_header)
    val auth       = new HmacCoreSecurity(config).authorization_header(hmac_req)

    all_header + ("Authorization" -> auth)
  }

  private[this] def buildHmacReq(
    job:        Job,
    config:     HmacSecurityConfig,
    all_header: Map[String, String]
  ): HmacSecurityRequest = {

    val custom_headers = all_header
      .filter(h => config.custom_headers.exists(_.equalsIgnoreCase(h._1)))
      .toList

    HmacSecurityRequest(
      job.method.productPrefix,
      None,
      None,
      all_header.get("Date").getOrElse(""),
      all_header.get("nonce").getOrElse(""),
      custom_headers
    )
  }

  private[this] def buildHmacConfig(credential: HmacCredential): HmacSecurityConfig = {
    HmacSecurity.parse_config(parse(credential))
  }

  private[this] def parse(credential: HmacCredential): Configuration = {
    Configuration.from(Map(
      "hmacsecurity.key"         -> credential.api_key,
      "hmacsecurity.secret"      -> credential.api_secret,
      "hmacsecurity.prefix"      -> credential.prefix.getOrElse("HMAC"),
      "hmacsecurity.time_window" -> credential.time_window.getOrElse(-1),
      "hmacsecurity.headers"     -> credential.headerKeys()
    ))
  }
}
