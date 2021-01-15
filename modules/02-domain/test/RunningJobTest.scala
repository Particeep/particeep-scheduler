package services

import org.scalatestplus.play.PlaySpec
import utils.TimeUtils
import domain._

object FiberHelperTest extends FiberHelper

class RunningJobTest extends PlaySpec {

  "RunningJob" should {

    "compute next execution time" in {

      val result1 = FiberHelperTest.compute_next_execution(
        frequency  = new Frequency("1 days"),
        started_at = TimeUtils.parse("2021-01-01T00:00:00Z").getOrElse(TimeUtils.now()),
        now        = TimeUtils.parse("2021-01-01T05:00:00Z").getOrElse(TimeUtils.now())
      )
      result1 mustBe TimeUtils.parse("2021-01-02T00:00:00Z")

      val result2 = FiberHelperTest.compute_next_execution(
        frequency  = new Frequency("1 minutes"),
        started_at = TimeUtils.parse("2021-01-01T00:00:00Z").getOrElse(TimeUtils.now()),
        now        = TimeUtils.parse("2021-01-01T05:00:00Z").getOrElse(TimeUtils.now())
      )

      result2 mustBe TimeUtils.parse("2021-01-01T05:00:00Z")

      val result3 = FiberHelperTest.compute_next_execution(
        frequency  = new Frequency("1 minutes"),
        started_at = TimeUtils.parse("2021-01-01T00:00:00Z").getOrElse(TimeUtils.now()),
        now        = TimeUtils.parse("2021-01-01T05:00:01Z").getOrElse(TimeUtils.now())
      )

      result3 mustBe TimeUtils.parse("2021-01-01T05:01:00Z")
    }
  }
}
