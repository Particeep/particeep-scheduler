package domain

import pl.iterators.kebs.tagged._
import utils.StringUtils
import utils.TimeUtils

import java.time.OffsetDateTime

import zio._

object RunningJobTag {
  trait RunningJobIdTag
  type RunningJobId = String @@ RunningJobIdTag

  object RunningJobId {
    def apply(arg: String) = from(arg)
    def from(arg:  String) = arg.taggedWith[RunningJobIdTag]
  }
}

case class RunningJobDisplay(
  id:          RunningJobTag.RunningJobId,
  job:         Job,
  started_at:  OffsetDateTime,
  next_run_at: Option[OffsetDateTime],
  status:      String,
  comment:     String
)

case class RunningJob(
  id:         RunningJobTag.RunningJobId = RunningJobTag.RunningJobId.from(StringUtils.generateUuid()),
  job:        Job,
  started_at: OffsetDateTime,
  fiber:      Fiber.Runtime[_, _]
) extends FiberHelper {

  def next_execution(): Option[OffsetDateTime] = compute_next_execution(job.frequency, started_at, TimeUtils.now())

  def format(): UIO[RunningJobDisplay] = {
    for {
      status    <- fiber.status
      status_str = status2str(status)
      dump      <- Fiber.dumpStr(fiber)
    } yield {
      RunningJobDisplay(id, job, started_at, next_execution(), status_str, dump)
    }
  }
}

trait FiberHelper {

  def compute_next_execution(
    frequency:  Frequency,
    started_at: OffsetDateTime,
    now:        OffsetDateTime
  ): Option[OffsetDateTime] = frequency.toDuration().map { frequency =>
    var result        = started_at
    val freq_in_nanos = frequency.toNanos
    while(result.isBefore(now)) {
      result = result.plusNanos(freq_in_nanos)
    }

    result
  }

  def status2str(status: Fiber.Status): String = {
    status match {
      case Fiber.Status.Done         => "Done"
      case _: Fiber.Status.Finishing => "Finishing"
      case _: Fiber.Status.Running   => "Running"
      case _: Fiber.Status.Suspended => "Suspended"
    }
  }
}
