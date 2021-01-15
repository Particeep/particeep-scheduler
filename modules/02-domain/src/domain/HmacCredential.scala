package domain

case class HmacCredential(
  api_key:     String,
  api_secret:  String,
  algo:        String               = "HMAC",
  prefix:      Option[String]       = None,
  time_window: Option[Int]          = None,
  headers:     Option[List[String]] = None
) {
  def headerMap(): Map[String, String] = {
    headers
      .map(_.flatMap { item =>
        item.split(":").toList match {
          case key :: value :: List() => Some(key -> value)
          case _                      => None
        }
      }.toMap)
      .getOrElse(Map.empty)
  }

  def headerKeys(): List[String] = headers.map(_.flatMap(_.split(":").headOption)).getOrElse(List())
}
