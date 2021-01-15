package domain

import pl.iterators.kebs.tagged._
import utils.StringUtils
import utils.TimeUtils

import java.time.OffsetDateTime

object JobTag {
  trait JobIdTag
  type JobId = String @@ JobIdTag

  object JobId {
    def apply(arg: String) = from(arg)
    def from(arg:  String) = arg.taggedWith[JobIdTag]
  }
}

case class Job(
  id:          JobTag.JobId           = JobTag.JobId.from(StringUtils.generateUuid()),
  created_at:  OffsetDateTime         = TimeUtils.now(),
  name:        String,
  start_time:  String,
  frequency:   Frequency,
  credentials: Option[HmacCredential] = None,
  url:         Url,
  method:      HttpMethod,
  tag:         List[String]           = List()
)

case class JobSearchCriteria(
  method: Option[HttpMethod] = None
)
