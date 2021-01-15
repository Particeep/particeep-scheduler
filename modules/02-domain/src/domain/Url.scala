package domain

class Url(val underlying: String) extends AnyVal {
  override def toString: String = underlying
}
