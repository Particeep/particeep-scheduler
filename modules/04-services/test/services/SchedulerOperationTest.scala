package services

import org.scalatestplus.play.PlaySpec

import domain.JobTag.JobId
import domain._
import effect.Fail
import play.api.Logging
import services.schedule.SchedulerOperation
import services.schedule.SchedulerOperation.JobScheduleInput
import test.ZioTestHelper
import utils.TimeUtils

import zio._

class SchedulerOperationTest extends PlaySpec with ZioTestHelper with Logging {
  def onScheduleError(fail: Fail, maybe_count: Option[Long]): UIO[Long] = {
    val count = maybe_count.getOrElse(0L)

    ZIO
      .effectTotal(println(s"Error running schedule after ${count} count with error ${fail.techMessages()}"))
      .map(_ => count)
  }

  "SchedulerOperation" should {

    "compute schedule input for a one time job" in {

      val job = Job(
        start_time  = "00:00:05",
        frequency   = Frequency("ONCE"),
        name        = "test-job",
        credentials = None,
        url         = new Url(""),
        method      = HttpMethod.GET
      )

      val now          = TimeUtils.parse("2020-01-01T00:00:00Z").getOrElse(TimeUtils.now())
      val result       = Runtime.default.unsafeRun(SchedulerOperation.computeScheduleInput(job, now))
      val initialDelay = zio.duration.Duration.fromMillis(5000)

      result mustBe JobScheduleInput(initialDelay, None)
    }

    "compute schedule input for wrong time" in {

      val job = Job(
        id          = JobId.from("1"),
        start_time  = "wrong time",
        frequency   = Frequency("ONCE"),
        name        = "test-job",
        credentials = None,
        url         = new Url(""),
        method      = HttpMethod.GET
      )

      val now    = TimeUtils.parse("2020-01-01T00:00:00Z").getOrElse(TimeUtils.now())
      val result = Runtime.default.unsafeRunSync(SchedulerOperation.computeScheduleInput(job, now))

      result.succeeded mustBe false
      result.mapError { err =>
        err mustBe Fail("Cron format 'wrong time' is not allowed")
          .withEx("Wrong Cron format for job 1")
      }
    }

    "compute schedule input for a recurring job" in {
      val job = Job(
        start_time  = "00:00:05",
        frequency   = Frequency("1 seconds"),
        name        = "test-job",
        credentials = None,
        url         = new Url(""),
        method      = HttpMethod.GET
      )

      val now          = TimeUtils.parse("2020-01-01T00:00:00Z").getOrElse(TimeUtils.now())
      val result       = Runtime.default.unsafeRun(SchedulerOperation.computeScheduleInput(job, now))
      val initialDelay = zio.duration.Duration.fromMillis(5000)
      val freq         = zio.duration.Duration.fromMillis(1000)

      result mustBe JobScheduleInput(initialDelay, Some(freq))
    }
  }
}
