package domain

import pl.iterators.kebs.tagged._
import utils.StringUtils

import java.time.OffsetDateTime

object ExecutionTag {
  trait ExecutionIdTag
  type ExecutionId = String @@ ExecutionIdTag

  object ExecutionId {
    def apply(arg: String) = from(arg)
    def from(arg:  String) = arg.taggedWith[ExecutionIdTag]
  }
}

case class Execution(
  id:          ExecutionTag.ExecutionId = ExecutionTag.ExecutionId.from(StringUtils.generateUuid()),
  job_id:      JobTag.JobId,
  executed_at: OffsetDateTime,
  status:      Int,
  response:    String
)

case class ExecutionSearchCriteria(
  runned_after:  Option[OffsetDateTime] = None,
  runned_before: Option[OffsetDateTime] = None,
  status:        Option[Int]            = None
)
