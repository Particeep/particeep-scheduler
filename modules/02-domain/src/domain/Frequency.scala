package domain

import scala.concurrent.duration._
import scala.util.Try

class Frequency(val underlying: String) extends AnyVal {
  override def toString: String = underlying

  def toDuration(): Option[Duration] = {
    underlying.toLowerCase() match {
      case "once" => None
      case _      => Try(Duration(underlying)).toOption
    }
  }
}

object Frequency {
  def apply(s: String): Frequency = new Frequency(s)
}
