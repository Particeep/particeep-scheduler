package filters

import filters.web._
import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.csp.CSPFilter
import play.filters.csrf.CSRFFilter
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter
import play.filters.hosts.AllowedHostsFilter

class AppliedFilters @Inject() (
  gzip:                  GzipFilter,
  allowedHostsFilter:    AllowedHostsFilter,
  cors:                  CORSFilter,
  csrf:                  CSRFFilter,
  expireSessionFilter:   ExpireSessionFilter,
  blackListCookieFilter: BlackListCookieFilter,
  security_headers:      SecurityHeadersFilter,
  csp:                   CSPFilter,
  session_filter:        SessionFilter
) extends HttpFilters {

  val filters = Seq(
    security_headers,
    // csp,
    allowedHostsFilter,
    cors,
    csrf,
    gzip,
    expireSessionFilter,
    blackListCookieFilter,
    session_filter
  )
}
