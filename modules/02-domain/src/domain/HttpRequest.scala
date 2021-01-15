package domain

case class HttpRequest(
  method:  HttpMethod,
  url:     Url,
  headers: Map[String, String]
)
