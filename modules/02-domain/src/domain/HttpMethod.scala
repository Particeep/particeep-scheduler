package domain

sealed trait HttpMethod extends Product with Serializable

object HttpMethod {

  final case object CONNECT extends HttpMethod
  final case object DELETE  extends HttpMethod
  final case object GET     extends HttpMethod
  final case object HEAD    extends HttpMethod
  final case object OPTIONS extends HttpMethod
  final case object PATCH   extends HttpMethod
  final case object POST    extends HttpMethod
  final case object PUT     extends HttpMethod
  final case object TRACE   extends HttpMethod

  val values = Set(CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE)

  def parse(s: String): Option[HttpMethod] = values.filter(_.productPrefix == s).headOption
}
