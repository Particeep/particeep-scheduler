package utils

import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time.{ Month, OffsetDateTime, ZoneOffset }

import scala.util.Try

object TimeUtils {

  private[this] val pattern: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(ZoneOffset.UTC)

  def from(year: Int, month: Month, day: Int): OffsetDateTime = {
    start_of_today().withYear(year).withMonth(month.getValue).withDayOfMonth(day)
  }

  def isIso(date: String): Boolean = parse(date).isDefined

  def parse(date: String): Option[OffsetDateTime] =
    Try {
      Some(OffsetDateTime.parse(date, pattern))
    }.getOrElse(None)

  def toIso(date: OffsetDateTime): String = pattern.format(date.withNano(0))

  def now(): OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC).withNano(0)

  def start_of_today(): OffsetDateTime = now().withHour(0).withMinute(0).withSecond(0).withNano(0)

  def now_iso(): String = toIso(now())

  def now_day(): String = {
    val date = now()
    s"${date.getYear}-${date.getMonth.getValue}-${date.getDayOfMonth}"
  }

  def format(date: OffsetDateTime, format: String): String = {
    DateTimeFormatter.ofPattern(format).format(date)
  }

  val format: Format[OffsetDateTime] = new Format[OffsetDateTime] {
    def reads(json: JsValue): JsResult[OffsetDateTime] = json match {
      case JsString(s) => parse(s).map(JsSuccess(_)).getOrElse(JsError(s"can't parse $s as ISO date"))
      case _           => JsError(s"can't parse $json as ISO date")
    }
    def writes(t:   OffsetDateTime): JsValue           = JsString(toIso(t))
  }
}
