package domain

import ai.x.play.json.Encoders._
import ai.x.play.json.Jsonx
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import tagging.AnyTypeclassTaggingCompat
import utils.json.OWritesOps._
import utils.json.AdtFormat

trait JsonParser extends AnyTypeclassTaggingCompat {

  implicit def searchWithTotalSizeWrites[T](implicit f: Writes[T]): Writes[SearchWithTotalSize[T]] = (
    (__ \ "total_size").write[Int] and
      (__ \ "data").write[List[T]]
  )((s: SearchWithTotalSize[T]) => (s.total_size, s.data))

  implicit def searchWithTotalSizeReads[T](implicit f:  Reads[T]): Reads[SearchWithTotalSize[T]]   = (
    (__ \ "total_size").read[Int] and
      (__ \ "data").read[List[T]]
  )((a, b) => SearchWithTotalSize[T](a, b))

  implicit val frequencyFormat    = Json.valueFormat[Frequency]
  implicit val urlFormat          = Json.valueFormat[Url]
  implicit val http_method_format = AdtFormat.format(HttpMethod.values)

  implicit val table_search_format = Jsonx.formatCaseClassUseDefaults[TableSearch]
  implicit val execution_format    = Jsonx.formatCaseClassUseDefaults[Execution]
  implicit val credentials_format  = Jsonx.formatCaseClassUseDefaults[HmacCredential]

  private[this] val jobs_reads  = Jsonx.formatCaseClassUseDefaults[Job]
  private[this] val jobs_writes = jobs_reads.removeField("credentials")
  implicit val jobs_format      = Format(jobs_reads, jobs_writes)

  implicit val running_job_format = Jsonx.formatCaseClassUseDefaults[RunningJobDisplay]
}
